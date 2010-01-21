/**
 * palava - a java-php-bridge
 * Copyright (C) 2007  CosmoCode GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package de.cosmocode.palava.services.mail;

public class EntityEncoder {

    private static final EntityEncoder instance = new EntityEncoder();
    
    private EntityEncoder() {
        
    }
    
    public String encode(String sourceString) {
        return sourceString.
            replace("&",  "&amp;").
            replace(">",  "&gt;").
            replace("<",  "&lt;").
            replace("'",  "&apos;").
            replace("\"", "&quot;").
            replace("%",  "&#37;");
    }

    public static EntityEncoder getInstance() {
        return instance; 
    }
    
}