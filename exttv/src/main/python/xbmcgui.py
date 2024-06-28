class DialogProgressBG:
    def __init__(self):
        pass

    def create(self, heading, message):
        print(f"Dialog OK - Heading: {heading}")
        print(f"message: {message}")

    def update(self, status, message=""):
        print(f"Dialog OK - Status {status} - {message}")
        return

class Dialog:
    def __init__(self):
        pass

    def ok(self, heading, line1, line2='', line3=''):
        print(f"Dialog OK - Heading: {heading}")
        print(f"Line 1: {line1}")
        if line2:
            print(f"Line 2: {line2}")
        if line3:
            print(f"Line 3: {line3}")

    def yesno(self, heading, line1, line2='', line3='', nolabel='No', yeslabel='Yes', autoclose=False):
        print(f"Dialog Yes/No - Heading: {heading}")
        print(f"Line 1: {line1}")
        if line2:
            print(f"Line 2: {line2}")
        if line3:
            print(f"Line 3: {line3}")
        print(f"No Label: {nolabel}")
        print(f"Yes Label: {yeslabel}")
        # Simulate a user response (for example, always returning True for Yes)
        return True

    def yesnocustom(self, heading, line1, line2='', line3='', nolabel='No', yeslabel='Yes', customlabel='Custom'):
        print(f"Dialog Yes/No/Custom - Heading: {heading}")
        print(f"Line 1: {line1}")
        if line2:
            print(f"Line 2: {line2}")
        if line3:
            print(f"Line 3: {line3}")
        print(f"No Label: {nolabel}")
        print(f"Yes Label: {yeslabel}")
        print(f"Custom Label: {customlabel}")
        # Simulate a user response (for example, always returning 'custom' for Custom)
        return 'custom'