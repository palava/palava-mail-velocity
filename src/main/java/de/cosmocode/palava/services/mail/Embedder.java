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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small helper class used inside templates to embed attachments.
 * 
 * @author Jesus Ortiz
 */
public class Embedder {
    
    private static final Logger LOG = LoggerFactory.getLogger(Embedder.class);

    // mapping  path => cid
    private final Map <String, String> embeddings = new HashMap<String, String>();
    private final List<String> filenames = new ArrayList<String>();
    private int cidCount;
    
    private final File resourcePath;
    
    public Embedder(File resourcePath) {
        this.resourcePath = resourcePath;
    }
    
    public Embedder(VelocityEngine engine) {
        this.resourcePath = new File(engine.getProperty("file.resource.loader.path").toString());
    }
    
    /**
     * Embeds an image at the specified path.
     * 
     * @param path the path
     * @return the content id
     */
    public String image(String path) {
        String cid = embeddings.get(path);

        if (cid == null) {
            cid = generateCID();
            setEmbedding(path, cid);
            LOG.debug("creating inline image; path=" + path + " cid=" + cid);
        } else {
            LOG.debug("reusing inline image; path=" +  path + " cid=" + cid);

        }
        return cid;
    }
    
    /**
     * Provides the file name of the given path.
     * 
     * @param path the path
     * @return the filename
     */
    public String name(String path) {
        String fileName = null;
        String cid = null;
        String[] parts = null;
        cid = embeddings.get(path);
        
        if (cid == null) {
            LOG.debug("Image " + path + " not embedded.");
            return path;
        }
        
        parts = path.split("/");
        fileName = parts[parts.length - 1];
        return fileName;
    }

    private String generateCID() {
        cidCount++;
        return System.currentTimeMillis() + "" + cidCount;
    }
    
    /**
     * Sets an embedding at the specified path.
     * 
     * @param path the path
     * @param cid the content id
     */
    public void setEmbedding(String path, String cid) {
        final String[] parts = path.split("/");
        final String fileName = parts[parts.length - 1];
        
        if (embeddings.get(path) == null && filenames.contains(fileName)) {
            throw new IllegalArgumentException(fileName + " was embedded twice from different paths");
        }
        
        filenames.add(fileName);
        embeddings.put(path, cid);
    }
    
    public Map<String, String> getEmbeddings() {
        return embeddings;
    }
    
    /**
     * Checks whether this embedder has embeddings.
     * 
     * @return true if has embeddings, false otherwise
     */
    public boolean hasEmbeddings() {
        return !embeddings.isEmpty();
    }
    
    public File getResourcePath() {
        return resourcePath;
    }
    
}
