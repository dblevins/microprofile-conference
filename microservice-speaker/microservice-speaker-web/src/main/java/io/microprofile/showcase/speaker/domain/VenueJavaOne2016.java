/*
 * Copyright 2016 Microprofile.io
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
package io.microprofile.showcase.speaker.domain;

import io.microprofile.showcase.speaker.model.Speaker;
import org.apache.commons.io.IOUtils;
import org.codehaus.swizzle.stream.IncludeFilterInputStream;
import org.codehaus.swizzle.stream.StreamLexer;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VenueJavaOne2016 extends Venue {

    private final Logger log = Logger.getLogger(VenueJavaOne2016.class.getName());

    private final URL url = URI.create("https://www.oracle.com/javaone/speakers.html").toURL();

    VenueJavaOne2016() throws MalformedURLException {
    }

    public static void main(final String[] args) {
        try {
            new VenueJavaOne2016().getSpeakers();
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "JavaOne2016";
    }

    public Set<Speaker> getSpeakers() {

        final Set<Speaker> speakers = new HashSet<>();

        InputStream is = null;

        try {
            is = this.url.openStream();
            is = new IncludeFilterInputStream(is, "<body", "</body>");
            final StreamLexer lexer = new StreamLexer(is);

            //Read all speaker <div> blocks
            String token;
            while (null != (token = lexer.readToken("<div class=\"cw16 cw16v1 cwidth\" ", "!-- CN"))) {
                speakers.add(this.processSpeakerToken(token));
            }

            //<div class="cw16 cw16v1 cwidth"

            //System.out.println(new String(readBytes(is)));

        } catch (final Exception e) {
            this.log.log(Level.SEVERE, "Failed to parse: " + this.url, e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (final Exception e) {
                    //no-op
                }
            }
        }

        return speakers;
    }

    private Speaker processSpeakerToken(final String token) {
        final Speaker speaker = new Speaker();
        speaker.setId(UUID.randomUUID().toString());
        //System.out.println("token = " + token);

        try (InputStream is = IOUtils.toInputStream(token, Charset.forName("UTF-8"))) {
            final StreamLexer lexer = new StreamLexer(is);
            speaker.setPicture(this.getImage(lexer));

            final String fullName = lexer.readToken("<h4>", "</h4>");
            speaker.setNameFirst(fullName.substring(0, fullName.indexOf(" ")));
            speaker.setNameLast(fullName.substring(fullName.indexOf(" ") + 1));

            speaker.setBiography(lexer.readToken("<p>", "</p>"));
            speaker.setTwitterHandle(lexer.readToken("href=\"https://twitter.com/", "\">@"));

        } catch (final Exception e) {
            this.log.log(Level.SEVERE, "Failed to parse token: " + token, e);
        }

        this.log.info("Found:" + speaker);

        return speaker;
    }

    private String getImage(final StreamLexer lexer) throws Exception {
        String imgToken = lexer.readToken("<div class=\"cw16w1\"><img", "</div>");
        imgToken = imgToken.substring(imgToken.indexOf("src=\"//") + 7);
        imgToken = "http://" + imgToken.substring(0, imgToken.indexOf("\""));
        return imgToken;
    }
}
