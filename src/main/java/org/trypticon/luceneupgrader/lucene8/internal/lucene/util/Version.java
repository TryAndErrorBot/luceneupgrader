/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trypticon.luceneupgrader.lucene8.internal.lucene.util;


import java.text.ParseException;
import java.util.Locale;

public final class Version {

    @Deprecated
    public static final Version LUCENE_7_0_0 = new Version(7, 0, 0);

    @Deprecated
    public static final Version LUCENE_7_0_1 = new Version(7, 0, 1);

    @Deprecated
    public static final Version LUCENE_7_1_0 = new Version(7, 1, 0);

    @Deprecated
    public static final Version LUCENE_7_2_0 = new Version(7, 2, 0);

    @Deprecated
    public static final Version LUCENE_7_2_1 = new Version(7, 2, 1);


    @Deprecated
    public static final Version LUCENE_7_3_0 = new Version(7, 3, 0);

    @Deprecated
    public static final Version LUCENE_7_3_1 = new Version(7, 3, 1);

    @Deprecated
    public static final Version LUCENE_7_4_0 = new Version(7, 4, 0);

    @Deprecated
    public static final Version LUCENE_7_5_0 = new Version(7, 5, 0);

    @Deprecated
    public static final Version LUCENE_7_6_0 = new Version(7, 6, 0);

    @Deprecated
    public static final Version LUCENE_7_7_0 = new Version(7, 7, 0);

    @Deprecated
    public static final Version LUCENE_7_7_1 = new Version(7, 7, 1);

    @Deprecated
    public static final Version LUCENE_7_7_2 = new Version(7, 7, 2);

    @Deprecated
    public static final Version LUCENE_7_7_3 = new Version(7, 7, 3);

    @Deprecated
    public static final Version LUCENE_7_8_0 = new Version(7, 8, 0);

    @Deprecated
    public static final Version LUCENE_8_0_0 = new Version(8, 0, 0);

    @Deprecated
    public static final Version LUCENE_8_1_0 = new Version(8, 1, 0);

    @Deprecated
    public static final Version LUCENE_8_1_1 = new Version(8, 1, 1);

    @Deprecated
    public static final Version LUCENE_8_2_0 = new Version(8, 2, 0);

    @Deprecated
    public static final Version LUCENE_8_3_0 = new Version(8, 3, 0);

    @Deprecated
    public static final Version LUCENE_8_3_1 = new Version(8, 3, 1);

    @Deprecated
    public static final Version LUCENE_8_4_0 = new Version(8, 4, 0);

    @Deprecated
    public static final Version LUCENE_8_4_1 = new Version(8, 4, 1);

    @Deprecated
    public static final Version LUCENE_8_5_0 = new Version(8, 5, 0);

    @Deprecated
    public static final Version LUCENE_8_5_1 = new Version(8, 5, 1);

    @Deprecated
    public static final Version LUCENE_8_5_2 = new Version(8, 5, 2);

    @Deprecated
    public static final Version LUCENE_8_6_0 = new Version(8, 6, 0);

    @Deprecated
    public static final Version LUCENE_8_6_1 = new Version(8, 6, 1);

    @Deprecated
    public static final Version LUCENE_8_6_2 = new Version(8, 6, 2);

    @Deprecated
    public static final Version LUCENE_8_6_3 = new Version(8, 6, 3);

    @Deprecated
    public static final Version LUCENE_8_7_0 = new Version(8, 7, 0);

    @Deprecated
    public static final Version LUCENE_8_8_0 = new Version(8, 8, 0);

    @Deprecated
    public static final Version LUCENE_8_8_1 = new Version(8, 8, 1);

    @Deprecated
    public static final Version LUCENE_8_8_2 = new Version(8, 8, 2);

    @Deprecated
    public static final Version LUCENE_8_9_0 = new Version(8, 9, 0);

    @Deprecated
    public static final Version LUCENE_8_10_0 = new Version(8, 10, 0);

    @Deprecated
    public static final Version LUCENE_8_10_1 = new Version(8, 10, 1);

    @Deprecated
    public static final Version LUCENE_8_11_0 = new Version(8, 11, 0);

    @Deprecated
    public static final Version LUCENE_8_11_1 = new Version(8, 11, 1);

    @Deprecated
    public static final Version LUCENE_8_11_2 = new Version(8, 11, 2);

    @Deprecated
    public static final Version LUCENE_9_0_0 = new Version(9, 0, 0);

    @Deprecated
    public static final Version LUCENE_9_1_0 = new Version(9, 1, 0);

    @Deprecated
    public static final Version LUCENE_9_2_0 = new Version(9, 2, 0);

    @Deprecated
    public static final Version LUCENE_9_3_0 = new Version(9, 3, 0);

    @Deprecated
    public static final Version LUCENE_9_4_0 = new Version(9, 4, 0);

    @Deprecated
    public static final Version LUCENE_9_4_1 = new Version(9, 4, 1);

    @Deprecated
    public static final Version LUCENE_9_4_2 = new Version(9, 4, 2);

    @Deprecated
    public static final Version LUCENE_9_5_0 = new Version(9, 5, 0);

    @Deprecated
    public static final Version LUCENE_9_6_0 = new Version(9, 6, 0);

    public static final Version LUCENE_9_7_0 = new Version(9, 7, 0);

    // To add a new version:
    //  * Only add above this comment
    //  * If the new version is the newest, change LATEST below and deprecate the previous LATEST

    public static final Version LATEST = LUCENE_9_7_0;

    @Deprecated
    public static final Version LUCENE_CURRENT = LATEST;

    public static Version parse(String version) throws ParseException {

        StrictStringTokenizer tokens = new StrictStringTokenizer(version, '.');
        if (tokens.hasMoreTokens() == false) {
            throw new ParseException("Version is not in form major.minor.bugfix(.prerelease) (got: " + version + ")", 0);
        }

        int major;
        String token = tokens.nextToken();
        try {
            major = Integer.parseInt(token);
        } catch (NumberFormatException nfe) {
            ParseException p = new ParseException("Failed to parse major version from \"" + token + "\" (got: " + version + ")", 0);
            p.initCause(nfe);
            throw p;
        }

        if (tokens.hasMoreTokens() == false) {
            throw new ParseException("Version is not in form major.minor.bugfix(.prerelease) (got: " + version + ")", 0);
        }

        int minor;
        token = tokens.nextToken();
        try {
            minor = Integer.parseInt(token);
        } catch (NumberFormatException nfe) {
            ParseException p = new ParseException("Failed to parse minor version from \"" + token + "\" (got: " + version + ")", 0);
            p.initCause(nfe);
            throw p;
        }

        int bugfix = 0;
        int prerelease = 0;
        if (tokens.hasMoreTokens()) {

            token = tokens.nextToken();
            try {
                bugfix = Integer.parseInt(token);
            } catch (NumberFormatException nfe) {
                ParseException p = new ParseException("Failed to parse bugfix version from \"" + token + "\" (got: " + version + ")", 0);
                p.initCause(nfe);
                throw p;
            }

            if (tokens.hasMoreTokens()) {
                token = tokens.nextToken();
                try {
                    prerelease = Integer.parseInt(token);
                } catch (NumberFormatException nfe) {
                    ParseException p = new ParseException("Failed to parse prerelease version from \"" + token + "\" (got: " + version + ")", 0);
                    p.initCause(nfe);
                    throw p;
                }
                if (prerelease == 0) {
                    throw new ParseException("Invalid value " + prerelease + " for prerelease; should be 1 or 2 (got: " + version + ")", 0);
                }

                if (tokens.hasMoreTokens()) {
                    // Too many tokens!
                    throw new ParseException("Version is not in form major.minor.bugfix(.prerelease) (got: " + version + ")", 0);
                }
            }
        }

        try {
            return new Version(major, minor, bugfix, prerelease);
        } catch (IllegalArgumentException iae) {
            ParseException pe = new ParseException("failed to parse version string \"" + version + "\": " + iae.getMessage(), 0);
            pe.initCause(iae);
            throw pe;
        }
    }

    public static Version parseLeniently(String version) throws ParseException {
        String versionOrig = version;
        version = version.toUpperCase(Locale.ROOT);
        switch (version) {
            case "LATEST":
            case "LUCENE_CURRENT":
                return LATEST;
            default:
                version = version
                        .replaceFirst("^LUCENE_(\\d+)_(\\d+)_(\\d+)$", "$1.$2.$3")
                        .replaceFirst("^LUCENE_(\\d+)_(\\d+)$", "$1.$2.0")
                        .replaceFirst("^LUCENE_(\\d)(\\d)$", "$1.$2.0");
                try {
                    return parse(version);
                } catch (ParseException pe) {
                    ParseException pe2 = new ParseException("failed to parse lenient version string \"" + versionOrig + "\": " + pe.getMessage(), 0);
                    pe2.initCause(pe);
                    throw pe2;
                }
        }
    }

    public static Version fromBits(int major, int minor, int bugfix) {
        return new Version(major, minor, bugfix);
    }

    public final int major;
    public final int minor;
    public final int bugfix;
    public final int prerelease;

    // stores the version pieces, with most significant pieces in high bits
    // ie:  | 1 byte | 1 byte | 1 byte |   2 bits   |
    //         major   minor    bugfix   prerelease
    private final int encodedValue;

    private Version(int major, int minor, int bugfix) {
        this(major, minor, bugfix, 0);
    }

    private Version(int major, int minor, int bugfix, int prerelease) {
        this.major = major;
        this.minor = minor;
        this.bugfix = bugfix;
        this.prerelease = prerelease;
        // NOTE: do not enforce major version so we remain future proof, except to
        // make sure it fits in the 8 bits we encode it into:
        if (major > 255 || major < 0) {
            throw new IllegalArgumentException("Illegal major version: " + major);
        }
        if (minor > 255 || minor < 0) {
            throw new IllegalArgumentException("Illegal minor version: " + minor);
        }
        if (bugfix > 255 || bugfix < 0) {
            throw new IllegalArgumentException("Illegal bugfix version: " + bugfix);
        }
        if (prerelease > 2 || prerelease < 0) {
            throw new IllegalArgumentException("Illegal prerelease version: " + prerelease);
        }
        if (prerelease != 0 && (minor != 0 || bugfix != 0)) {
            throw new IllegalArgumentException("Prerelease version only supported with major release (got prerelease: " + prerelease + ", minor: " + minor + ", bugfix: " + bugfix + ")");
        }

        encodedValue = major << 18 | minor << 10 | bugfix << 2 | prerelease;

        assert encodedIsValid();
    }

    public boolean onOrAfter(Version other) {
        return encodedValue >= other.encodedValue;
    }

    @Override
    public String toString() {
        if (prerelease == 0) {
            return "" + major + "." + minor + "." + bugfix;
        }
        return "" + major + "." + minor + "." + bugfix + "." + prerelease;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Version && ((Version) o).encodedValue == encodedValue;
    }

    // Used only by assert:
    private boolean encodedIsValid() {
        assert major == ((encodedValue >>> 18) & 0xFF);
        assert minor == ((encodedValue >>> 10) & 0xFF);
        assert bugfix == ((encodedValue >>> 2) & 0xFF);
        assert prerelease == (encodedValue & 0x03);
        return true;
    }

    @Override
    public int hashCode() {
        return encodedValue;
    }
}
