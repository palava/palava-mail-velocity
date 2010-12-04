/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.services.mail;

/**
 * A small helper class used inside template to encode certain xml entities.
 * 
 * @author Willi Schoenborn
 */
public final class EntityEncoder {

    private static final EntityEncoder INSTANCE = new EntityEncoder();
    
    private EntityEncoder() {
        
    }
    
    /**
     * Encodes the given string.
     * 
     * @param source the source string
     * @return the encoded version
     */
    public String encode(String source) {
        return source == null ? "" : source.
            replace("&",  "&amp;").
            replace(">",  "&gt;").
            replace("<",  "&lt;").
            replace("'",  "&apos;").
            replace("\"", "&quot;").
            replace("%",  "&#37;");
    }

    public static EntityEncoder getInstance() {
        return INSTANCE; 
    }
    
}
