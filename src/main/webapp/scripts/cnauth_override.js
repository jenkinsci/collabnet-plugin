// get the proper authentication-related strings
function get_auth_param_str(prefix) {
    var str = "";
    var override = true;
    var override_elems = document.getElementsByName(prefix + ".override_auth");
    if (override_elems.length == 0) {
        // if there is no override choice, then we are always overriding.
        override = true;
    } else {
        override = override_elems[0].checked;
    }
    if (override == true) {
        str = 'url=' + 
            escape(document.getElementById(prefix + '.collabneturl').value) + 
            '&username=' + 
            escape(document.getElementById(prefix + '.username').value) +
            '&password='+ 
            escape(document.getElementById(prefix + '.password').value) + 
            '&';
    }
    str += 'override_auth=' + override;
    return str;
}