import os
import utils
import traceback

from java import jclass

toast_utils = jclass("com.android.exttv.util.ToastUtils")

utils.init(os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../'))) # very important to normalize

def run(argv=""):
    try:
        utils.run([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else ""])
    except Exception as e:
        traceback.print_exception(type(e), e, e.__traceback__)
        tb = traceback.extract_tb(e.__traceback__)  # Get the traceback
        filename, line_number, func_name, _ = tb[-1]
        toast_utils.showToast(f"{filename}:{line_number} ({func_name})\n{e}", 1)
