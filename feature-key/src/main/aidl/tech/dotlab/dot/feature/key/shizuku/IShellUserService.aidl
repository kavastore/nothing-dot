package tech.dotlab.dot.feature.key.shizuku;

interface IShellUserService {
    // Reserved transaction id Shizuku calls to tear the service down.
    void destroy() = 16777114;

    // Runs `pm enable` (enabled=true) or `pm disable-user --user 0` (enabled=false) under shell uid.
    boolean setPackageEnabled(String packageName, boolean enabled) = 1;
}
