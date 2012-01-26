package org.commonjava.maven.plugins.arqas;

import java.io.File;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

public abstract class AbstractArqASGoal
    implements Mojo
{

    /**
     * File location (default: target/jbossas) where resolved JBossAS distribution should be unpacked.
     * 
     * @parameter default-value="${project.build.directory}/jbossas" expression="${arqas.output}"
     */
    protected File output;

    private Log log;

    @Override
    public final synchronized Log getLog()
    {
        if ( log == null )
        {
            log = new SystemStreamLog();
        }

        return log;
    }

    @Override
    public final void setLog( final Log log )
    {
        this.log = log;
    }

}
