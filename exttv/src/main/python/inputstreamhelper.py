class Helper:
    """The main InputStream Helper class"""

    def __init__(self, protocol, drm=None):
        """Initialize InputStream Helper class"""
        if drm == 'com.widevine.alpha':
            self.inputstream_addon = "inputstream.adaptive"
            self.protocol = protocol
            self.drm = drm
        else:
            Exception("Unknown DRM system")

    def check_inputstream(self):
        """Check if InputStream Helper is supported"""
        return True