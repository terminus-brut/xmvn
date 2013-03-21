/*-
 * Copyright (c) 2013 Red Hat, Inc.
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
package org.fedoraproject.maven.tools.bisect;

import java.util.Map.Entry;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.fedoraproject.maven.utils.AtomicFileCounter;

/**
 * @author Mikolaj Izdebski
 */
@Component( role = BisectCli.class )
public class BisectCli
{
    @Requirement
    private Logger logger;

    @Requirement
    private CommandLineParser commandLineParser;

    @Requirement
    private BuildExecutor buildExecutor;

    public void run( String[] args )
        throws Exception
    {
        commandLineParser.parseCommandLine( args );
        InvocationRequest request = commandLineParser.createInvocationRequest();
        request.setShellEnvironmentInherited( true );

        for ( Entry<String, String> entry : commandLineParser.getSystemProperties().entrySet() )
            System.setProperty( entry.getKey(), entry.getValue() );

        String counterPath = "/home/kojan/git/xmvn/bisect";
        request.addShellEnvironment( "M2_HOME", commandLineParser.getSystemProperties().get( "maven.home" ) );
        request.getProperties().put( "xmvn.bisect.repository", "/home/kojan/.m2" );
        request.getProperties().put( "xmvn.bisect.counter", counterPath );

        int counterInitialValue = 1000000000;
        AtomicFileCounter counter = new AtomicFileCounter( counterPath, counterInitialValue );

        int badId = 0;
        logger.info( "Running initial upstream build" );
        boolean success = buildExecutor.executeBuild( request, "bisect-initial.log", commandLineParser.isVerbose() );
        int goodId = counterInitialValue - counter.getValue();
        if ( !success )
        {
            logger.fatalError( "Build failed even when resolving artifacts completely from bisection repository" );
            System.exit( 1 );
        }

        while ( goodId - badId > 1 )
        {
            int tryId = badId + 1;
            if ( commandLineParser.useBinarySearch() )
                tryId += ( goodId - badId ) / 2;

            logger.info( "Bisection iteration: current range is [" + ( badId + 1 ) + "," + ( goodId - 1 )
                + "], trying " + tryId );
            counter.setValue( tryId );

            success = buildExecutor.executeBuild( request, "bisect-" + tryId + ".log", commandLineParser.isVerbose() );
            logger.info( "Bisection build number " + tryId + " " + ( success ? "succeeded" : "failed" ) );

            if ( success )
                goodId = tryId;
            else
                badId = tryId;
        }

        String goodLog = "bisect-" + goodId + ".log";
        if ( goodId == counterInitialValue )
            goodLog = "bisect-initial.log";
        String badLog = "bisect-" + badId + ".log";
        if ( badId == 0 )
            badLog = "default.log";

        logger.info( "Bisection build finished" );
        logger.info( "Successful build: " + ( goodId - 1 ) + ", see " + goodLog );
        logger.info( "Failed build:     " + ( badId + 1 ) + ", see " + badLog );
        logger.info( "Try:" );
        logger.info( "  $ git diff --no-index --color " + badLog + " " + goodLog );
    }

    public static void main( String[] args )
    {
        PlexusContainer container = null;
        try
        {
            container = new DefaultPlexusContainer();
            BisectCli cli = container.lookup( BisectCli.class );
            cli.run( args );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
    }
}