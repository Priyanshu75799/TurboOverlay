package com.PriyanshuDeveloper.turbooverlay;

interface IPrivilegedShellService {
    String exec(String command);
    String readSystemFile(String path);
    void destroy();
}
