import utils
import os.path
from urllib.parse import urlparse

class Stat:
    def __init__(self, path):
        self.path = path
        self._stat_result = os.stat(path)
    
    def st_size(self):
        return self._stat_result.st_size
    
    def st_mtime(self):
        return self._stat_result.st_mtime
    
    def st_ctime(self):
        return self._stat_result.st_ctime
    
    def st_mode(self):
        return self._stat_result.st_mode
    
    def st_uid(self):
        return self._stat_result.st_uid
    
    def st_gid(self):
        return self._stat_result.st_gid
    
class File:
    def __init__(self, path, permissions='r'):
        self.path = path
        self.permissions = permissions
        self.file_object = None
        self._opened = False
    
    def exists(self):
        return os.path.exists(self.path)
    
    def size(self):
        return os.path.getsize(self.path)
    
    def rename(self, new_path):
        os.rename(self.path, new_path)
        self.path = new_path
    
    def delete(self):
        if os.path.isfile(self.path):
            os.remove(self.path)
        elif os.path.isdir(self.path):
            shutil.rmtree(self.path)
        else:
            raise ValueError(f"Path '{self.path}' does not exist or is not a valid file or directory.")
    
    def copy(self, dst):
        if os.path.isdir(self.path):
            shutil.copytree(self.path, dst)
        else:
            shutil.copy2(self.path, dst)
    
    def stat(self):
        return Stat(self.path)
    
    def open(self):
        if not self._opened:
            self.file_object = open(self.path, self.permissions)
            self._opened = True
    
    def close(self):
        if self._opened:
            self.file_object.close()
            self._opened = False
    
    def read(self, size=-1):
        self.open()
        try:
            return self.file_object.read(size)
        except Exception as e:
            raise e
        finally:
            self.close()
    
    def write(self, data):
        self.open()
        try:
            self.file_object.write(data)
        except Exception as e:
            raise e
        finally:
            self.close()

def exists(path):
    # Check if path is a URL
    parsed = urlparse(path)
    if parsed.scheme in ('http', 'https'):
        return True
    
    # Check if path is a local file
    if os.path.isfile(path):
        return True
    
    # Return False if neither a URL nor a local file exists
    return False

def delete(path):
    if os.path.isfile(path):
        os.remove(path)
    elif os.path.isdir(path):
        shutil.rmtree(path)
    else:
        raise ValueError(f"Path '{path}' does not exist or is not a valid file or directory.")

def rename(src, dst):
    os.rename(src, dst)

def copy(src, dst):
    if os.path.isdir(src):
        shutil.copytree(src, dst)
    else:
        shutil.copy2(src, dst)

def rmdir(path):
    os.rmdir(path)

def mkdir(path):
    try:
        if not os.path.exists(path):
            os.mkdir(path)
        return True
    except Exception as e:
        print(f"Error creating directory: {e}")
        return False

def mkdirs(path):
    try:
        if not os.path.exists(path):
            os.makedirs(path)
        return True
    except Exception as e:
        print(f"Error creating directories: {e}")
        return False

def listdir(path):
    folders = []
    files = []
    for item in os.listdir(path):
        item_path = os.path.join(path, item)
        if os.path.isdir(item_path):
            folders.append(item)
        else:
            files.append(item)
    return folders, files

def translatePath(path):
    if 'special://' in path:
        if path.startswith('special://profile/'):
            return path.replace('special://profile/', utils.full_userdata_path())
        else:
            return path.replace('special://', utils.full_home_path())
    else:
        return path

