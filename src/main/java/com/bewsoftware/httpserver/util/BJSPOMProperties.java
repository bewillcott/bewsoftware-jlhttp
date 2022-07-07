/*
 *  File Name:    NewClass.java
 *  Project Name: bewsoftware-jlhttp
 *
 *  Copyright (c) 2022 Bradley Willcott
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bewsoftware.httpserver.util;

import java.io.IOException;
import java.util.Properties;

import static com.bewsoftware.httpserver.util.Constants.DISPLAY;
import static com.bewsoftware.httpserver.util.Keys.*;

/**
 * Provides access to some of the project's pom.properties.
 * <p>
 * To setup in a new project:</p>
 * <ol>
 * <li>Create a new file: "pom.properties" <br>
 * Location: "src/main/resources"
 * <p>
 * </li>
 * <li><p>
 * Place the following text into that file, and save it:</p>
 * <pre>
 *<code>
 * name=${project.name}
 * description=${project.description}
 * artifactId=${project.artifactId}
 * groupId=${project.groupId}
 * version=${project.version}
 * filename=${project.build.finalName}.jar
 * </code>
 * </pre>
 * </li><li><p>
 * Add the following to your projects <b>pom.xml</b> file:</p>
 * <pre>
 *<code>
 *&lt;build&gt;
 *    &lt;resources&gt;
 *        ...
 *        &lt;resource&gt;
 *            &lt;directory&gt;src/main/resources&lt;/directory&gt;
 *            &lt;filtering&gt;true&lt;/filtering&gt;
 *            &lt;includes&gt;
 *                &lt;include&gt;&#42;&#42;/pom.properties&lt;/include&gt;
 *            &lt;/includes&gt;
 *        &lt;/resource&gt;
 *        &lt;resource&gt;
 *            &lt;directory&gt;src/main/resources&lt;/directory&gt;
 *            &lt;filtering&gt;false&lt;/filtering&gt;
 *            &lt;excludes&gt;
 *                &lt;exclude&gt;&#42;&#42;/pom.properties&lt;/exclude&gt;
 *            &lt;/excludes&gt;
 *        &lt;/resource&gt;
 *    &lt;/resources&gt;
 *        ...
 *&lt;/build&gt;
 *</code>
 * </pre>
 * </li>
 * </ol>
 * <p>
 * To access the properties:
 * </p>
 * <pre><code>
 * POMProperties pom = POMProperties.INSTANCE;
 * DISPLAY.println(pom.name):
 * </code></pre>
 *
 * @author <a href="mailto:bw.opensource@yahoo.com">Bradley Willcott</a>
 *
 * @since 2.6.3
 * @version 2.6.3
 */
public final class BJSPOMProperties
{

    /**
     * Provides single instance of this class.
     */
    public static final BJSPOMProperties INSTANCE = new BJSPOMProperties();

    /**
     * The identifier for this artifact that is unique within
     * the group given by the group ID.
     */
    public final String artifactId;

    /**
     * Project Description
     */
    public final String description;

    /**
     * The filename of the binary output file.
     * <p>
     * This is usually a '.jar' file.
     * </p>
     */
    public final String filename;

    /**
     * Project GroupId
     */
    public final String groupId;

    /**
     * Project Name
     */
    public final String name;

    /**
     * The version of the artifact.
     */
    public final String version;

    private BJSPOMProperties()
    {
        Properties properties = new Properties();

        try
        {
            properties.load(BJSPOMProperties.class.getResourceAsStream("/bjspom.properties"));
        } catch (IOException ex)
        {
            throw new RuntimeException("FileIOError", ex);
        }

        name = properties.getProperty(NAME);
        description = properties.getProperty(DESCRIPTION);
        groupId = properties.getProperty(GROUP_ID);
        artifactId = properties.getProperty(ARTIFACT_ID);
        version = properties.getProperty(VERSION);
        filename = properties.getProperty(FILENAME);
    }

    public static void main(String[] args)
    {
        DISPLAY.level(0).println(BJSPOMProperties.INSTANCE);
    }

    @Override
    public String toString()
    {
        return new StringBuilder(BJSPOMProperties.class.getName()).append(":\n")
                .append("  name: ").append(name).append("\n")
                .append("  description: ").append(description).append("\n")
                .append("  groupId: ").append(groupId).append("\n")
                .append("  artifactId: ").append(artifactId).append("\n")
                .append("  version: ").append(version).append("\n")
                .append("  filename: ").append(filename).append("\n").toString();
    }
}
