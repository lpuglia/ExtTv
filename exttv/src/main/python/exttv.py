import os
import time
import utils
import traceback
# from joblib import Parallel, delayed

from java import jclass
from java.util import Arrays

toast_utils = jclass("com.android.exttv.util.ToastUtils")

utils.init(os.path.normpath(os.path.join(os.path.dirname(__file__),'../../../'))) # very important to normalize

# def single_run(argv=""):
#     print("LOL", [argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else "?"])
#     item_list = []
#     try:
#         to_return = utils.run([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else ""])
#         for item in to_return:
#             video = item[1].info.get('video', {})
#             plot = video.get('plot', '') if video else ''
#             item_list.append([
#                 item[0],
#                 item[1].label if item[1].label else '',
#                 item[1].label2 if item[1].label2 else '',
#                 plot,
#                 item[1].art.get('thumb', ''),
#                 item[1].art.get('poster', ''),
#                 item[1].art.get('fanart', ''),
#                 item[2], # isFolder
#                 utils.parent_uri_map[item[0]],
#                 utils.parent_uri_map[item[0]],
#                 "", "", int(time.time()), False, None
#             ])
#     except Exception as e:
#         traceback.print_exception(type(e), e, e.__traceback__)
#         toast_utils.showToast(str(e), 1)
#     return item_list
#
# def multi_run(argvs):
#     argvs = list(argvs.toArray())
#     results = Parallel(n_jobs=7)(delayed(single_run)(arg) for arg in argvs)
#     cardView = jclass("com.android.exttv.model.data.CardItem")
#     item_list = [cardView(*item) for sublist in results for item in sublist]
#     return Arrays.asList(item_list)

def run(argv=""):
    print([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else "?"])
    item_list = []
    try:
        to_return = utils.run([argv.split("?")[0], '3', argv[argv.find("?"):] if "?" in argv else ""])
        cardView = jclass("com.android.exttv.model.data.CardItem")
        for item in to_return:
            video = item[1].info.get('video', {})
            plot = video.get('plot', '') if video else ''
            item_list.append(cardView(
                item[0],
                item[1].label if item[1].label else '',
                item[1].label2 if item[1].label2 else '',
                plot,
                item[1].art.get('thumb', ''),
                item[1].art.get('poster', ''),
                item[1].art.get('fanart', ''),
                item[2], # isFolder
                utils.parent_uri_map[item[0]],
                utils.parent_uri_map[item[0]],
                "", "", int(time.time()), False, None
            ))
    except Exception as e:
        traceback.print_exception(type(e), e, e.__traceback__)
        exc_type, exc_value, exc_tb = traceback.exc_info()
        file_name = exc_tb.tb_frame.f_code.co_filename
        line_number = exc_tb.tb_lineno
        toast_utils.showToast(f"Exception occurred in file {file_name} at line {line_number}: {e}", 1)
    return Arrays.asList(item_list)
