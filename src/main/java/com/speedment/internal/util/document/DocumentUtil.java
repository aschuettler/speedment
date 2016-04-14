/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.internal.util.document;

import com.speedment.config.Document;
import com.speedment.config.db.trait.HasAlias;
import com.speedment.config.db.trait.HasName;
import com.speedment.config.db.trait.HasParent;
import com.speedment.internal.util.Trees;
import com.speedment.stream.MapStream;
import static com.speedment.util.NullUtil.requireNonNulls;
import static com.speedment.util.StaticClassUtil.instanceNotAllowed;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;

/**
 * Common utility methods for working with instances of the {@code Document}
 * interface.
 * 
 * @author Per Minborg
 * @author Emil Forslund
 */
public final class DocumentUtil {

    /**
     * Traverses all the documents below the specified document in the tree.
     * Traversal is done depth first.
     * 
     * @param document  the document to start at
     * @return          stream of descendants
     */
    @SuppressWarnings("unchecked")
    public static Stream<? extends Document> traverseOver(Document document) {
        requireNonNull(document);
        return Trees.traverse(
            document,
            d -> (Stream<Document>) d.children(),
            Trees.TraversalOrder.DEPTH_FIRST_PRE
        );
    }

    /**
     * Returns the first ancestor found of the specified type to the specified
     * document when walking up the tree. If there was no ancestor of the
     * specified type and the root was reached, an empty {@code Optional} is
     * returned.
     * 
     * @param <E>       ancestor type
     * @param document  the starting point
     * @param clazz     the ancestor type to look for
     * @return          first ancestor found or empty
     */
    public static <E extends Document> Optional<E> ancestor(
            Document document, 
            Class<E> clazz) {
        
        requireNonNulls(document, clazz);
        return document.ancestors()
            .filter(clazz::isInstance)
            .map(clazz::cast)
            .findFirst();
    }

    /**
     * Returns a stream of child documents to a specified document by using the
     * supplied constructor.
     * 
     * @param <E>               the expected child type
     * @param document          the parent document
     * @param childConstructor  child constructor
     * @return 
     */
    @SuppressWarnings("unchecked")
    public static <E extends Document> Stream<E> childrenOf(
            Document document, 
            BiFunction<Document, Map<String, Object>, E> childConstructor) {
        
        return document.stream().values()
            .filter(obj -> obj instanceof List<?>)
            .map(list -> (List<Object>) list)
            .flatMap(list -> list.stream())
            .filter(obj -> obj instanceof Map<?, ?>)
            .map(map -> (Map<String, Object>) map)
            .map(map -> childConstructor.apply(document, map));
    }

    /**
     * Creates and returns a new raw map on a specified key in the specified
     * document. This might involve creating a new list if no such existed
     * already. If children aldready existed on that key, the new one is simply
     * added to the end of the list.
     * 
     * @param parent  the parent to create it in
     * @param key     the key to create it under
     * @return        the newly creating raw child map
     */
    public static Map<String, Object> newDocument(Document parent, String key) {
        final List<Map<String, Object>> children = parent.get(key)
            .map(Document.DOCUMENT_LIST_TYPE::cast)
            .orElseGet(() -> {
                final List<Map<String, Object>> list = new CopyOnWriteArrayList<>();
                parent.put(key, list);
                return list;
            });

        final Map<String, Object> child = new ConcurrentHashMap<>();
        children.add(child);

        return child;
    }
    
    /**
     * An enumeration of the types of names that documents can have. This is
     * used to control which method should be called when parsing the document
     * into a name.
     */
    public enum Name {
        
        /**
         * The name used in the database to reference this document.
         */
        DATABASE_NAME,
        
        /**
         * A user defined name that is used for the document primarily in 
         * generated code.
         */
        JAVA_NAME;
        
        /**
         * Returns the appropriate name of the specified document.
         * 
         * @param document  the document
         * @return          the name
         */
        public String of(HasAlias document) {
            switch (this) {
                case DATABASE_NAME : return document.getName();
                case JAVA_NAME     : return document.getJavaName();
                default : throw new UnsupportedOperationException(
                    "Unknown enum constant '" + name() + "'."
                );
            }
        }
    }

    /**
     * Returns the relative name for the given Document up to the point given by
     * the parent Class.
     * <p>
     * For example, {@code relativeName(column, Dbms.class, DATABASE_NAME)} 
     * would return the String "dbms_name.schema_name.table_name.column_name".
     *
     * @param <T>      parent type
     * @param <D>      document type
     * @param document to use
     * @param from     class
     * @return         the relative name for this Node from the point given by 
     *                 the parent Class
     */
    public static <T extends Document & HasName, D extends Document & HasName> 
    String relativeName(D document, Class<T> from, Name name) {
        return relativeName(document, from, name, Function.identity());
    }

    /**
     * Returns the relative name for the given Document up to the point given by
     * the parent Class by successively applying the provided nameMapper onto
     * the Node names.
     * <p>
     * For example, {@code relativeName(column, Dbms.class, DATABASE_NAME)} 
     * would return the String "dbms_name.schema_name.table_name.column_name".
     *
     * @param <T> parent type
     * @param <D> Document type
     * @param document to use
     * @param from class
     * @param nameMapper to apply to all names encountered during traversal
     * @return the relative name for this Node from the point given by the
     * parent Class
     */
    public static <T extends Document & HasName, D extends Document & HasName> 
    String relativeName(
            D document, 
            Class<T> from, 
            Name name, 
            Function<String, String> nameMapper) {
        
        return relativeName(document, from, name, ".", nameMapper);
    }

    /**
     * Returns the relative name for the given Document up to the point given by
     * the parent Class by successively applying the provided nameMapper onto
     * the Node names and separating the names with the provided separator.
     * <p>
     * For example, {@code relativeName(column, Dbms.class)} would return the
     * String "dbms_name.schema_name.table_name.column_name" if the separator is
     * "."
     *
     * @param <T> parent type
     * @param <D> Document type
     * @param document to use
     * @param from class
     * @param separator to use between the document names
     * @param nameMapper to apply to all names encountered during traversal
     * @return the relative name for this Node from the point given by the
     * parent Class
     */
    public static <T extends Document & HasName, D extends Document & HasName> String relativeName(
            D document,
            Class<T> from,
            Name name,
            CharSequence separator,
            Function<String, String> nameMapper) {
        
        requireNonNulls(document, from, nameMapper);
        final StringJoiner sj = new StringJoiner(separator).setEmptyValue("");
        final List<HasAlias> ancestors = document.ancestors()
            .map(HasAlias::of)
            .collect(toList());
        
        boolean add = false;
        for (final HasAlias parent : ancestors) {
            if (add || parent.mainInterface().isAssignableFrom(from)) {
                sj.add(nameMapper.apply(name.of(parent)));
                add = true;
            }
        }
        sj.add(nameMapper.apply(name.of(HasAlias.of(document))));
        return sj.toString();
    }
    
    /**
     * Creates a deep copy of the raw map in the specified document and wrap it 
     * in a new typed document using the specified constructor.
     * 
     * @param <P>          the parent type
     * @param <DOC>        the document type
     * @param document     the document
     * @param constructor  the document constructor
     * @return             the copy
     */
    public static <DOC extends Document> DOC deepCopy(
            DOC document, 
            Function<Map<String, Object>, DOC> constructor) {
        
        return constructor.apply(deepCopyMap(document.getData()));
    }
    
    /**
     * Creates a deep copy of the raw map in the specified document and wrap it 
     * in a new typed document using the specified constructor.
     * 
     * @param <P>          the parent type
     * @param <DOC>        the document type
     * @param document     the document
     * @param constructor  the document constructor
     * @return             the copy
     */
    public static <P extends Document, DOC extends Document & HasParent<P>> 
    DOC deepCopy(DOC document, BiFunction<P, Map<String, Object>, DOC> constructor) {
        
        return constructor.apply(
            document.getParent().orElse(null), 
            deepCopyMap(document.getData())
        );
    }
    
    /**
     * Returns an {@code Exception} supplier for when no attribute could be
     * found on a specified key in a specified document.
     * 
     * @param document  the document
     * @param key       the key
     * @return          the {@code Exception} supplier
     */
    public static Supplier<NoSuchElementException> newNoSuchElementExceptionFor(
            Document document, 
            String key) {
        
        return () -> new NoSuchElementException(
                "An attribute with the key '" + key
                + "' could not be found in " + document
                + " with name (" + Optional.ofNullable(document)
                    .flatMap(doc -> doc.getAsString("name"))
                    .orElse("null")
                + ")"
        );
    }
    
    /**
     * Helps documents to format a {@code toString()}-method.
     * 
     * @param document  the document
     * @return          the string
     */
    public static String toStringHelper(Document document) {

        return document.getClass().getSimpleName()
            + " {"
            + MapStream.of(document.getData())
            .mapValue(VALUE_MAPPER)
            .map((k, v) -> "\"" + k + "\": " + v.toString())
            .collect(joining(", "))
            + "}";
    }
    
    private static <K, V> Map<K, V> deepCopyMap(Map<K, V> original) {
        final Map<K, V> copy = new ConcurrentHashMap<>();
        
        MapStream.of(original)
            .mapValue(DocumentUtil::deepCopyObject)
            .forEachOrdered(copy::put);
        
        return copy;
    }
    
    private static <V> List<V> deepCopyList(List<V> original) {
        final List<V> copy = new CopyOnWriteArrayList<>();
        
        original.stream()
            .map(DocumentUtil::deepCopyObject)
            .forEachOrdered(copy::add);
        
        return copy;
    }

    private static <V> V deepCopyObject(V original) {
        if (String.class.isAssignableFrom(original.getClass())
        ||  Number.class.isAssignableFrom(original.getClass())
        ||  Boolean.class.isAssignableFrom(original.getClass())
        ||  Enum.class.isAssignableFrom(original.getClass())) {
            return original;
        } else if (List.class.isAssignableFrom(original.getClass())) {
            @SuppressWarnings("unchecked")
            final V result = (V) deepCopyList((List<?>) original);
            return result;
        } else if (Map.class.isAssignableFrom(original.getClass())) {
            @SuppressWarnings("unchecked")
            final V result = (V) deepCopyMap((Map<?, ?>) original);
            return result;
        } else {
            throw new UnsupportedOperationException(
                "Can't deep copy unknown type '" + original.getClass() + "'."
            );
        }
    }
    
    private static final Function<Object, Object> VALUE_MAPPER = o -> {
        if (o instanceof List) {
            return "[" + ((List) o).size() + "]";
        } else {
            return o;
        }
    };

    /**
     * Utility classes should not be instantiated.
     */
    private DocumentUtil() {
        instanceNotAllowed(getClass());
    }
}
