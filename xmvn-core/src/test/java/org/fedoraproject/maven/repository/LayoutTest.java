/*-
 * Copyright (c) 2012-2013 Red Hat, Inc.
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
package org.fedoraproject.maven.repository;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;
import org.fedoraproject.maven.model.Artifact;

/**
 * @author Mikolaj Izdebski
 */
public class LayoutTest
    extends PlexusTestCase
{
    @Requirement
    private Repository defaultRepository;

    @Requirement( hint = MavenRepository.ROLE_HINT )
    private Repository mavenRepository;

    @Requirement( hint = JppRepository.ROLE_HINT )
    private Repository jppRepository;

    @Requirement( hint = FlatRepository.ROLE_HINT )
    private Repository flatRepository;

    /**
     * Make sure there is no default repository.
     * 
     * @throws Exception
     */
    public void defaultRepositoryTest()
        throws Exception
    {
        assertNull( defaultRepository );
    }

    private void testPaths( Repository repository, Artifact artifact, String... result )
    {
        Set<String> expected = new TreeSet<>();
        Set<String> actual = new TreeSet<>();

        expected.addAll( Arrays.asList( result ) );
        for ( Path path : repository.getArtifactPaths( artifact ) )
            actual.add( path.toString() );

        assertEquals( expected, actual );
    }

    /**
     * Test layout objects.
     * 
     * @throws Exception
     */
    public void testLayouts()
        throws Exception
    {
        Artifact artifact = new Artifact( "an-example.artifact", "used-FOR42.testing", "blah-1.2.3-foo", "ext-ens.ion" );

        testPaths( mavenRepository, artifact,
                   "an-example/artifact/used-FOR42.testing/blah-1.2.3-foo/used-FOR42.testing-blah-1.2.3-foo.ext-ens.ion" );
        testPaths( mavenRepository, artifact.clearVersion() );
        testPaths( jppRepository, artifact, "an-example.artifact/used-FOR42.testing-blah-1.2.3-foo.ext-ens.ion" );
        testPaths( jppRepository, artifact.clearVersion(), "an-example.artifact/used-FOR42.testing.ext-ens.ion" );
        testPaths( flatRepository, artifact, "an-example.artifact-used-FOR42.testing-blah-1.2.3-foo.ext-ens.ion" );
        testPaths( flatRepository, artifact.clearVersion(), "an-example.artifact-used-FOR42.testing.ext-ens.ion" );
    }

    /**
     * Test is JPP prefixes in groupId are handled correctly.
     * 
     * @throws Exception
     */
    public void testJppPrefixes()
        throws Exception
    {
        Artifact artifact1 = new Artifact( "JPP", "testing", "1.2.3", "abc" );
        Artifact artifact2 = new Artifact( "JPP/group", "testing", "1.2.3", "abc" );
        Artifact artifact3 = new Artifact( "JPP-group", "testing", "1.2.3", "abc" );

        testPaths( jppRepository, artifact1.clearVersion(), "testing.abc" );
        testPaths( jppRepository, artifact2.clearVersion(), "group/testing.abc" );
        testPaths( jppRepository, artifact3.clearVersion(), "JPP-group/testing.abc" );
    }
}
