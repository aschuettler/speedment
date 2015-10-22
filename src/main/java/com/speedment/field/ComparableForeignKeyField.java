/**
 *
 * Copyright (c) 2006-2015, Speedment, Inc. All Rights Reserved.
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
package com.speedment.field;

import com.speedment.annotation.Api;
import com.speedment.field.trait.ComparableFieldTrait;
import com.speedment.field.trait.FieldTrait;
import com.speedment.field.trait.ReferenceFieldTrait;
import com.speedment.field.trait.ReferenceForeignKeyFieldTrait;

/**
 *
 * @author pemi, Emil Forslund
 * @param <ENTITY> the entity type
 * @param <V> the field value type
 * @param <FK> the foreign entity type
 */
@Api(version = "2.2")
public interface ComparableForeignKeyField<ENTITY, V extends Comparable<? super V>, FK> extends
    FieldTrait,
    ReferenceFieldTrait<ENTITY, V>,
    ComparableFieldTrait<ENTITY, V>,
    ReferenceForeignKeyFieldTrait<ENTITY, FK>
{}