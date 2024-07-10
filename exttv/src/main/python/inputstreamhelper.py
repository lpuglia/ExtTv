class Helper:
    """The main InputStream Helper class"""

    def __init__(self, protocol, drm=None):
        """Initialize InputStream Helper class"""
        self.protocol = protocol
        self.drm = drm

    def check_inputstream(self):
        """Check if InputStream Helper is supported"""
        return True