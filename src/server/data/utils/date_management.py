import datetime as dt

def throw_error(error_message : str, obj):
    """NOT IMPLEMENTED. Should throw an error"""
    return

def date_from_string(s : str):
    """Returns a YYYY-MM-DD string as a datetime.date"""
    if not isinstance(s, str):
        throw_error("Input is not a string", s)
        return
    if len(s) != 10:
        throw_error("Input has incorrect length", s)
        return
    if not (s[0:4] + s[5:7] + s[8:10]).isdigit():
        throw_error("Input is not in correct format", s)
        return
    if s[4] != "-" or s[7] != "-":
        throw_error("Input is not in correct format", s)
        return
    return dt.date(s[0:4], s[5:7], s[8:10])

def date_from_string_or_none(s : str):
    """returns date_from_string(s) if s != "" else None"""
    if s:
        return date_from_string(s)
    else:
        return None