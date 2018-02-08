/*
 * Copyright [2016] [George Papadakis (gpapadis@yahoo.gr)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package DataReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author G.A.P. II
 */

public abstract class AbstractReader implements IDataReader {
    
    private static final Logger LOGGER = Logger.getLogger(AbstractReader.class.getName());
    
    protected String inputFilePath;
    
    public AbstractReader (String filePath) {
        inputFilePath = filePath;
    }
    
    public static Object loadSerializedObject(String fileName) {
        Object object = null;
        try {
            InputStream file = new FileInputStream(fileName);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            try {
                object = input.readObject();
            } finally {
                input.close();
            }
        } catch (ClassNotFoundException cnfEx) {
            LOGGER.log(Level.SEVERE, null, cnfEx);
            return null;
        } catch (IOException ioex) {
            LOGGER.log(Level.SEVERE, null, ioex);
            return null;
        }

        return object;
    }
    
    public void convertToRDFfile(List<EntityProfile> profiles, String outputPath) {
        try {
    	    FileWriter fileWriter = new FileWriter(outputPath);
    	    PrintWriter printWriter = new PrintWriter(fileWriter);
    	    printWriter.println("<?xml version=\"1.0\"?>");
    	    printWriter.println();
    	    printWriter.println("<rdf:RDF");
    	    printWriter.println("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
    	    printWriter.println("xmlns:obj=\"https://www.w3schools.com/rdf/\">");
    	    for (EntityProfile profile : profiles) {
          	  printWriter.println("<rdf:Description rdf:about=\""+profile.getEntityUrl().replace("&", "")+"\">");
              for (Attribute attribute : profile.getAttributes()) {
            	  printWriter.print("<obj:"+attribute.getName().replace("&", "")+">");
            	  printWriter.print(attribute.getValue().replace("&", ""));
            	  printWriter.println("</obj:"+attribute.getName().replace("&", "")+">");
              }
        	  printWriter.println("</rdf:Description>");

          }
    	    printWriter.println("</rdf:RDF>");
    	    printWriter.close();
        } catch (IOException ioex) {
            LOGGER.log(Level.SEVERE, null, ioex);
        }
    }
    
    @Override
    public void storeSerializedObject(Object object, String outputPath) {
        try {
            OutputStream file = new FileOutputStream(outputPath);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            try {
                output.writeObject(object);
            } finally {
                output.close();
            }
        } catch (IOException ioex) {
            LOGGER.log(Level.SEVERE, null, ioex);
        }
    }
}
