/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.java2d.macos;

import java.security.PrivilegedAction;
import sun.java2d.metal.MTLGraphicsConfig;
import sun.java2d.opengl.CGLGraphicsConfig;


public class MacOSFlags {

    /**
     * Description of command-line flags.  All flags with [true|false]
     * values
     *      metalEnabled: usage: "-Dsun.java2d.metal=[true|false]"
     */

    private static boolean oglEnabled;
    private static boolean oglVerbose;
    private static boolean metalEnabled;
    private static boolean metalVerbose;

    private enum PropertyState {ENABLED, DISABLED, UNSPECIFIED};

    static {
        initJavaFlags();
        initNativeFlags();
    }

    private static native boolean initNativeFlags();

    private static PropertyState getBooleanProp(String p, PropertyState defaultVal) {
        String propString = System.getProperty(p);
        PropertyState returnVal = defaultVal;
        if (propString != null) {
            if (propString.equals("true") ||
                propString.equals("t") ||
                propString.equals("True") ||
                propString.equals("T") ||
                propString.equals("")) // having the prop name alone
            {                          // is equivalent to true
                returnVal = PropertyState.ENABLED;
            } else if (propString.equals("false") ||
                       propString.equals("f") ||
                       propString.equals("False") ||
                       propString.equals("F"))
            {
                returnVal = PropertyState.DISABLED;
            }
        }
        return returnVal;
    }

    private static boolean isBooleanPropTrueVerbose(String p) {
        String propString = System.getProperty(p);
        if (propString != null) {
            if (propString.equals("True") ||
                propString.equals("T"))
            {
                return true;
            }
        }
        return false;
    }


    private static boolean getPropertySet(String p) {
        String propString = System.getProperty(p);
        return (propString != null) ? true : false;
    }

    private static void initJavaFlags() {
        java.security.AccessController.doPrivileged(
                (PrivilegedAction<Object>) () -> {
                    PropertyState oglState = getBooleanProp("sun.java2d.opengl", PropertyState.UNSPECIFIED);
                    PropertyState metalState = getBooleanProp("sun.java2d.metal", PropertyState.UNSPECIFIED);

                    if (metalState == PropertyState.UNSPECIFIED) {
                        if (oglState == PropertyState.DISABLED) {
                            oglEnabled = false;
                            metalEnabled = true;
                        } else if (oglState == PropertyState.ENABLED || oglState == PropertyState.UNSPECIFIED) {
                            oglEnabled = true;
                            metalEnabled = false;
                        }
                    } else if (metalState == PropertyState.ENABLED) {
                        if (oglState == PropertyState.DISABLED || oglState == PropertyState.UNSPECIFIED) {
                            oglEnabled = false;
                            metalEnabled = true;
                        } else if (oglState == PropertyState.ENABLED) {
                            oglEnabled = true;
                            metalEnabled = false;
                        }
                    } else if (metalState == PropertyState.DISABLED) {
                        oglEnabled = true;
                        metalEnabled = false;
                    }

                    oglVerbose = isBooleanPropTrueVerbose("sun.java2d.opengl");
                    metalVerbose = isBooleanPropTrueVerbose("sun.java2d.metal");

                    boolean oglAvailable = CGLGraphicsConfig.isCGLAvailable();
                    boolean metalAvailable = MTLGraphicsConfig.isMetalAvailable();

                    if (!oglAvailable && !metalAvailable) {
                        // Should never reach here
                        throw new RuntimeException("Error - Both, OpenGL and Metal frameworks not available.");
                    }

                    if (oglEnabled && !metalEnabled) {
                        // Check whether OGL is available
                        if (!oglAvailable) {
                            if (oglVerbose) {
                                System.out.println("Could not enable OpenGL pipeline (CGL not available)");
                            }
                            oglEnabled = false;
                            metalEnabled = metalAvailable;
                        }
                    } else if (metalEnabled && !oglEnabled) {
                        // Check whether Metal framework is available
                        if (!metalAvailable) {
                            if (metalVerbose) {
                                System.out.println("Could not enable Metal pipeline (Metal framework not available)");
                            }
                            metalEnabled = false;
                            oglEnabled = oglAvailable;
                        }
                    }

                    // At this point one of the rendering pipeline must be enabled.
                    if (!metalEnabled && !oglEnabled) {
                        throw new RuntimeException("Error - unable to initialize any rendering pipeline.");
                    }

                    return null;
                });
    }

    public static boolean isMetalEnabled() {
        return metalEnabled;
    }

    public static boolean isMetalVerbose() {
        return metalVerbose;
    }

    public static boolean isOGLEnabled() {
        return oglEnabled;
    }

    public static boolean isOGLVerbose() {
        return oglVerbose;
    }
}
