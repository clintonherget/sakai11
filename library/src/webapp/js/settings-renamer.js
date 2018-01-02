var OLD_TOOL_LABEL = 'Settings';
var SETTINGS_TOOL_LABEL = 'Site Groups';

var SETTINGS_ICON_CSS_CLASS = 'icon-sakai--sakai-siteinfo';
var JOINABLE_GROUPS_ICON_CSS_CLASS = 'icon-sakai--sakai-joinable-groups';
var SETTINGS_ICON_SELECTOR = '.icon-sakai--sakai-siteinfo';

function renameSettingsToJoinable(title) {
    if (showSiteInfoAsSettings || title != OLD_TOOL_LABEL) {
        return title;
    } else {
        return SETTINGS_TOOL_LABEL;
    }
}


var findSettingsMenuLink = function () {
    var toolMenu = $('#toolMenu');
    var elt = $(SETTINGS_ICON_SELECTOR, toolMenu).closest('.Mrphs-toolsNav__menuitem--link');

    if (elt.length > 0) {
        return elt;
    } else {
        return undefined;
    }
};

var prepareTheToolPage = function(tool) {
    // change the header site-hierarchy refresh button (mobile)
    var navReset = $('.Mrphs-siteHierarchy .Mrphs-hierarchy-item.Mrphs-hierarchy--toolName');
    navReset.find('span:last-child').text(SETTINGS_TOOL_LABEL);
    navReset.find(SETTINGS_ICON_SELECTOR).removeClass(SETTINGS_ICON_CSS_CLASS).addClass(JOINABLE_GROUPS_ICON_CSS_CLASS);

    // change the tool-scoped refresh button (desktop)
    var nyuRefresh = $('.nyu-desktop-only .Mrphs-hierarchy-item.Mrphs-hierarchy--toolName');
    nyuRefresh.find('span:last-child').text(SETTINGS_TOOL_LABEL);
    nyuRefresh.find(SETTINGS_ICON_SELECTOR).removeClass(SETTINGS_ICON_CSS_CLASS).addClass(JOINABLE_GROUPS_ICON_CSS_CLASS);

    // hide things in the sites table
    $('.summary-mathjax-allowed', tool).remove();
    $('.summary-instruction-mode', tool).remove();
};

var switchToJoinableGroups = function (link) {
    var title = $(link).find('.Mrphs-toolsNav__menuitem--title');
    title.text(SETTINGS_TOOL_LABEL)

    var iconSpan = $(link).find('.Mrphs-toolsNav__menuitem--icon');
    iconSpan.removeClass(SETTINGS_ICON_SELECTOR).addClass(JOINABLE_GROUPS_ICON_CSS_CLASS);
};


var markAsHidden = function (elt) {
    $(elt).addClass("is-invisible");
};


$(document).ready(function() {
    var settingsTool = findSettingsMenuLink();

    if (!settingsTool) {
        return;
    }

    if (showSiteInfoAsSettings) {
        markAsHidden(settingsTool);
        return;
    }

    if (showJoinableGroups) {
        switchToJoinableGroups(settingsTool);

        var tool = $('.Mrphs-sakai-siteinfo');
        if (tool.length > 0) {
            prepareTheToolPage(tool);
        }
    } else {
        $(settingsTool).closest('li').hide();
    }
});
