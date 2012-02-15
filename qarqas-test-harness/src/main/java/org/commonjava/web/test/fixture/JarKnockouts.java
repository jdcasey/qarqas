/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.web.test.fixture;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarKnockouts
{

    private final Set<String> knockoutPaths = new HashSet<String>();

    public JarKnockouts( final Class<?>... knockoutClasses )
    {
        for ( final Class<?> cls : knockoutClasses )
        {
            this.knockoutPaths.add( pathOf( cls ) );
        }
    }

    public JarKnockouts knockoutPaths( final String... paths )
    {
        for ( final String path : paths )
        {
            this.knockoutPaths.add( path );
        }

        return this;
    }

    public JarKnockouts knockoutClasses( final Class<?>... classes )
    {
        for ( final Class<?> cls : classes )
        {
            this.knockoutPaths.add( pathOf( cls ) );
        }

        return this;
    }

    public void rewriteJar( final File source, final File targetDir )
        throws IOException
    {
        targetDir.mkdirs();
        final File target = new File( targetDir, source.getName() );

        JarFile in = null;
        JarOutputStream out = null;
        try
        {
            in = new JarFile( source );

            final BufferedOutputStream fos = new BufferedOutputStream( new FileOutputStream( target ) );
            out = new JarOutputStream( fos, in.getManifest() );

            final Enumeration<JarEntry> entries = in.entries();
            while ( entries.hasMoreElements() )
            {
                final JarEntry entry = entries.nextElement();
                if ( !knockout( entry.getName() ) )
                {
                    final InputStream stream = in.getInputStream( entry );
                    out.putNextEntry( entry );
                    copy( stream, out );
                    out.closeEntry();
                }
            }
        }
        finally
        {
            closeQuietly( out );
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( final IOException e )
                {
                }
            }
        }
    }

    public static File rewriteJar( final File source, final File targetDir, final Set<JarKnockouts> jarKnockouts )
        throws IOException
    {
        final JarKnockouts allKnockouts = new JarKnockouts();
        for ( final JarKnockouts jk : jarKnockouts )
        {
            allKnockouts.knockoutPaths( jk.getKnockedOutPaths() );
        }

        targetDir.mkdirs();
        final File target = new File( targetDir, source.getName() );

        JarFile in = null;
        JarOutputStream out = null;
        try
        {
            in = new JarFile( source );

            final BufferedOutputStream fos = new BufferedOutputStream( new FileOutputStream( target ) );
            out = new JarOutputStream( fos, in.getManifest() );

            final Enumeration<JarEntry> entries = in.entries();
            while ( entries.hasMoreElements() )
            {
                final JarEntry entry = entries.nextElement();
                if ( !allKnockouts.knockout( entry.getName() ) )
                {
                    final InputStream stream = in.getInputStream( entry );
                    out.putNextEntry( entry );
                    copy( stream, out );
                    out.closeEntry();
                }
            }
        }
        finally
        {
            closeQuietly( out );
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( final IOException e )
                {
                }
            }
        }

        return target;
    }

    public String[] getKnockedOutPaths()
    {
        return this.knockoutPaths.toArray( new String[] {} );
    }

    public boolean knockout( final String path )
    {
        return knockoutPaths.contains( path );
    }

    private String pathOf( final Class<?> cls )
    {
        return cls.getName()
                  .replace( '.', '/' ) + ".class";
    }

}
