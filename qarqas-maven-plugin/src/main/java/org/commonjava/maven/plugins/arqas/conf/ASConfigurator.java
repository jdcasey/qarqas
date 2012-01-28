package org.commonjava.maven.plugins.arqas.conf;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public interface ASConfigurator
{

    Map<String, String> configure( File jbossasDir, Properties config, Log log )
        throws MojoExecutionException;

    void cleanup( File jbossasDir, Properties config, Log log );

}
