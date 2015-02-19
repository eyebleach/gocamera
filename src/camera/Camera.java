/*
Gocamera - Gopro compatible camera control library
Copyright (C) 2015 Patrik Hoggren

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package camera;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * GoPro compatible camera control-library over WiFi.
 * Client must be connected to cameras WiFi-AP.
 */
public class Camera {
    String cameraPwd;
    
    /**
     * Initate Camera class.
     */
    public Camera() {
        cameraPwd = loadCameraPwd();
    }
    
    /**
     * Check if camera is loaded and ready to control.
     * @return True if loaded.
     */
    public boolean isLoaded() {
        if(cameraPwd.equals("")) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Returns a HashMap with various statuses.<br><br>
     * <b>MenuItem</b> - 0: Video, 1: Photo, 2: Burst, 3: Timelapse, 7: Settings.<br>
     * <b>Battery</b> - Battery left in per cent.<br>
     * <b>PhotosLeft</b> - Photos left.<br>
     * <b>PhotoCount</b> - Photos on sd-card.<br>
     * <b>VideosLeft</b> - Video minutes left.<br>
     * <b>VideoCount</b> - Videos on sd-card.<br>
     * <b>Recording</b> - 1: recording, 0: not recording
     * @return Status hashmap with values.
     */
    public HashMap<String,Integer> getStatus() {
        HashMap<String,Integer> status = new HashMap<>();
        ArrayList<Integer> ret = executeCmd("http://10.5.5.9/camera/se?t=" + cameraPwd);
        
        status.put("MenuItem", ret.get(1));
        status.put("Battery", ret.get(19));
        status.put("PhotosLeft", Integer.parseInt(Integer.toHexString(ret.get(21)) + Integer.toHexString(ret.get(22)),16));
        status.put("PhotoCount", Integer.parseInt(Integer.toHexString(ret.get(23)) + Integer.toHexString(ret.get(24)),16));
        status.put("VideosLeft", Integer.parseInt(Integer.toHexString(ret.get(25)) + Integer.toHexString(ret.get(26)),16));
        status.put("VideoCount", Integer.parseInt(Integer.toHexString(ret.get(27)) + Integer.toHexString(ret.get(28)),16));
        status.put("Recording", ret.get(29));
        
        return status;
    }
    
    /* DEBUG
    public String getMediaList() { "http://10.5.5.9:8080/gp/gpMediaList" }
    */
    
    /**
     * Get camera-name, not same as the WiFi-name.
     * @return Camera-name.
     */
    public String getCameraName() {
        ArrayList<Integer> arrName;
        String name = "";
        int firmwareLen;
        int namePos;
        int nameLen;
        
        if((arrName = executeCmd("http://10.5.5.9/camera/cv")) != null) {
            
            firmwareLen = arrName.get(3);
            namePos = firmwareLen + 5;
            nameLen = arrName.get(4 + firmwareLen);
            
            for(int i = namePos; i < (namePos+nameLen); i++) {
                name += (char)arrName.get(i).intValue();
            }
        }
        return name;
    }
    
    /**
     * Get firmware-version.
     * @return Firmware-version.
     */
    public String getFirmware() {
        ArrayList<Integer> arrName;
        String name = "";
        int firmwareLen;
        int namePos;
        int nameLen;

        if((arrName = executeCmd("http://10.5.5.9/camera/cv")) != null) {
            
            firmwareLen = arrName.get(3);
            namePos = firmwareLen + 5;
            nameLen = arrName.get(4 + firmwareLen);
            
            for(int i = 4; i < (4 + firmwareLen); i++) {
                name += (char)arrName.get(i).intValue();
            }
        }
        
        return name;
    }
    
    /**
     * Sets capture mode to video.
     * @return True if ok.
     */
    public boolean setVideoMode() {
        if(executeCmd("http://10.5.5.9/camera/CM?t=" + cameraPwd + "&p=%00") == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Sets capture mode to photo.
     * @return True if ok.
     */
    public boolean setPhotoMode() {
        if(executeCmd("http://10.5.5.9/camera/CM?t=" + cameraPwd + "&p=%01") == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Sets capture mode to burst.
     * @return True if ok.
     */
    public boolean setBurstMode() {
        if(executeCmd("http://10.5.5.9/camera/CM?t=" + cameraPwd + "&p=%02") == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Sets capture mode to timelapse.
     * @return True if ok.
     */
    public boolean setTimelapse() {
        if(executeCmd("http://10.5.5.9/camera/CM?t=" + cameraPwd + "&p=%03") == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Fire of shutter.
     * Equalient to pressing top-button on the camera.
     */
    public void shutter() {
        executeCmd("http://10.5.5.9/bacpac/SH?t=" + cameraPwd + "&p=%01");
    }
    
    /**
     * Stop current capture.
     */
    public void stop() {
        executeCmd("http://10.5.5.9/bacpac/SH?t=" + cameraPwd + "&p=%00");
    }
    
    private String loadCameraPwd() {
        String password = "";
        ArrayList<Integer> pwd = executeCmd("http://10.5.5.9/bacpac/sd");
        
        if(pwd == null) {
            System.err.println("ERROR: Could not get password from camera.");
        } else {
            pwd.remove(0);
            pwd.remove(0);

            for(Integer i : pwd) {
                password += (char)i.intValue();
            }
        }
        
        return password;
    }
    
    private ArrayList<Integer> executeCmd(String cmd) {
        ArrayList<Integer> ret = null;
        
        try {
            URL url = new URL(cmd);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(1000);
            
            InputStreamReader instr = new InputStreamReader(conn.getInputStream(),"ISO-8859-1");
            
            ret = new ArrayList<>();
            
            int buf = instr.read();
            
            while(buf >= 0) {
                ret.add(buf);
                buf = instr.read();
            }
            
            instr.close();
            
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL: " + cmd + ". " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }
        
        return ret;
    }
    /* DEBUG
    private String getFile(String cmd) {
        String content = "";
        
        try {
            URL url = new URL(cmd);
            
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setChunkedStreamingMode(0);
            conn.connect();
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
           
            char[] buf = new char[4096];
            int len = 0;
            
            while(in.read(buf, 0, buf.length) >= 0) {
                System.out.print("hej");
            }
            
            content = String.valueOf(buf);
            
            in.close();
            
                        
        } catch(MalformedURLException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
     return content;   
    }
    */
    
}
