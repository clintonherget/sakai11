(function() {
    var LESSONS_SUBPAGE_NAVIGATION_LABELS = {
        expand:                     'Expand to show subpages',
        collapse:                   'Collapse to hide subpages',
        open_top_level_page:        'Click to open top-level page',
        hidden:                     ' [Hidden]',
        hidden_with_release_date:   ' [Not released until {releaseDate}]'
    };

    var LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH = 90;

    function LessonsSubPageNavigation(data, currentPageId) {
        this.data = data;
        this.currentPageId = currentPageId;
        this.has_current = false;
        this.page_id_to_submenu_item = {};
        this.setup();
    };

    LessonsSubPageNavigation.prototype.setup = function() {
        var self = this;

        for (var page_id in self.data) {
            if (self.data.hasOwnProperty(page_id)) {
                var sub_pages = self.data[page_id];
                self.render_subnav_for_page(page_id, sub_pages);
            }
        }
    };

    LessonsSubPageNavigation.prototype.render_subnav_for_page = function(page_id, sub_pages) {
        var self = this;

        var submenu_id = "lessonsSubMenu_" + page_id;

        var $menu = document.querySelector('#toolMenu a[href$="/tool/'+page_id+'"], #toolMenu [href$="/tool-reset/'+page_id+'"]');
        var $li = $menu.parentElement;

        var $submenu = document.createElement('ul');
        $submenu.classList.add('lessons-sub-page-menu');
        $submenu.setAttribute('aria-hidden', true);
        $submenu.style.display = 'none';
        $submenu.id = submenu_id;

        sub_pages.forEach(function(sub_page) {
            var $submenu_item = document.createElement('li');
            var $submenu_action = document.createElement('a');

            $submenu_action.href = self.build_sub_page_url_for(sub_page);
            $submenu_action.innerText = sub_page.name;
            $submenu_action.setAttribute('data-sendingPage', sub_page.sendingPage);

            var title_string = sub_page.name;
            if (title_string.length < LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH - 20) { // only show description if there's room
                if (sub_page.description) {
                  if (sub_page.description.length > (LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH - title_string.length)) {
                    title_string += " - " + sub_page.description.substring(0, LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH) + "...";
                  } else {
                    title_string += " - " + sub_page.description;
                  }
                }
            }

            if (sub_page.hidden == 'true') {
                $submenu_action.classList.add('is-invisible');
                if (sub_page.releaseDate) {
                    title_string += LESSONS_SUBPAGE_NAVIGATION_LABELS.hidden_with_release_date.replace(/\{releaseDate\}/, sub_page.releaseDate);
                } else {
                    title_string += LESSONS_SUBPAGE_NAVIGATION_LABELS.hidden;
                }
            }

            $submenu_action.title = title_string;

            $submenu_item.appendChild($submenu_action);

            $submenu.appendChild($submenu_item);

            if (sub_page.sendingPage === self.currentPageId) {
                $li.classList.add('is-parent-of-current');
                $submenu_item.classList.add('is-current');
                self.has_current = true;
            }

            self.page_id_to_submenu_item[sub_page.sendingPage] = $submenu_item;
        });

        $li.appendChild($submenu);
        self.setup_parent_menu($li, $menu, submenu_id);
    };


    LessonsSubPageNavigation.prototype.expand = function($expandMe, doNotAnimate, callback) {
        $expandMe.hide().show(0);
        $expandMe.addClass('sliding-down');
        $expandMe.find('.lessons-sub-page-menu').slideDown((doNotAnimate == true) ? 0 : 500, function() {
            var $submenu = $(this);

            $expandMe.removeClass('sliding-down');
            $expandMe.addClass('expanded');

            var $expandMeLink = $expandMe.find('> a.Mrphs-toolsNav__menuitem--link');
            var $placeholderMenuLink = $expandMe.find('> span.Mrphs-toolsNav__menuitem--link');

            $expandMeLink.hide().attr('aria-hidden', true);
            $placeholderMenuLink.show().attr('aria-hidden', false);

            $submenu.attr('aria-hidden', false);

            // To better provide screenreader continuity, focus the collapse button
            // as the expand button is hidden from the user and can no longer be clicked
            // Also, blur it soon after so mouse users don't get a facefull of focus-outline
            $placeholderMenuLink.find('.lessons-expand-collapse-icon').focus();
            setTimeout(function() {
                $placeholderMenuLink.find('.lessons-expand-collapse-icon').blur();
            });

            if (callback) {
                setTimeout(callback);
            }
        });
    };

    LessonsSubPageNavigation.prototype.collapse = function($collapseMe, callback) {
        $collapseMe.addClass('sliding-up');
        $collapseMe.find('.lessons-sub-page-menu').slideUp(500, function() {
            var $submenu = $(this);

            $collapseMe.removeClass('sliding-up');
            $collapseMe.removeClass('expanded');

            var $expandMeLink = $collapseMe.find('> a.Mrphs-toolsNav__menuitem--link');
            var $placeholderMenuLink = $collapseMe.find('> span.Mrphs-toolsNav__menuitem--link');

            $expandMeLink.show().attr('aria-hidden', false);
            $placeholderMenuLink.hide().attr('aria-hidden', true);

            $submenu.attr('aria-hidden', true);

            // To better provide screenreader continuity, focus the expand button
            // as the collapse button is hidden from the user and can no longer be clicked
            // Also, blur it soon after so mouse users don't get a facefull of focus-outline
            $expandMeLink.focus();
            setTimeout(function() {
                $expandMeLink.blur();
            });

            if (callback) {
                setTimeout(callback);
            }
        });
    };


    LessonsSubPageNavigation.prototype.setup_parent_menu = function($li, $menu, submenu_id) {
        var self = this;

        // stash the top level Lessons page URL
        var topLevelPageHref = $menu.href;
        // force it to be a tool-reset so the session state/breadcrumb
        // is cleared when visiting this top level page
        topLevelPageHref = topLevelPageHref.replace(/\/tool\//, "/tool-reset/");

        // add a wrapper CSS class so we can style things fancy-like
        $li.classList.add('has-lessons-sub-pages');

        // create a span to replace the original top level icon
        // it will contain two links, one to collapse the menu and another to visit the lessons page
        var $expandedMenuPlaceholder = document.createElement('span');
        $expandedMenuPlaceholder.classList.add('Mrphs-toolsNav__menuitem--link');
        $expandedMenuPlaceholder.classList.add('lessons-top-level-placeholder');
        $expandedMenuPlaceholder.style.display = 'none';

        // create a link to close an expanded menu
        var $collapseToggle = document.createElement('a');
        $collapseToggle.setAttribute('href', 'javascript:void(0);');
        $collapseToggle.setAttribute('aria-controls', submenu_id);
        $collapseToggle.setAttribute('aria-expanded', true);
        $collapseToggle.setAttribute('title', LESSONS_SUBPAGE_NAVIGATION_LABELS.collapse);
        $collapseToggle.classList.add("lessons-expand-collapse-icon");
        $collapseToggle.innerHTML = $menu.querySelector('.Mrphs-toolsNav__menuitem--icon').outerHTML;
        $expandedMenuPlaceholder.appendChild($collapseToggle);

        // create a link to go to the top level page (only visible when expanded)
        var $expandedGoToTopItem = document.createElement('a');
        $expandedGoToTopItem.setAttribute('href', topLevelPageHref);
        $expandedGoToTopItem.setAttribute('title', LESSONS_SUBPAGE_NAVIGATION_LABELS.open_top_level_page);
        $expandedGoToTopItem.classList.add("lessons-goto-top-page");
        $expandedGoToTopItem.innerHTML = $menu.querySelector('.Mrphs-toolsNav__menuitem--title').outerHTML;
        $expandedMenuPlaceholder.appendChild($expandedGoToTopItem);

        // insert the placeholder menu item before the $menu link
        $li.insertBefore($expandedMenuPlaceholder, $menu);

        $menu.href = 'javascript:void(0);';
        $menu.setAttribute('aria-controls', submenu_id);
        $menu.setAttribute('aria-expanded', false);
        $menu.setAttribute('aria-hidden', false);
        $menu.setAttribute('title', LESSONS_SUBPAGE_NAVIGATION_LABELS.expand);

        $menu.addEventListener('click', function(event) {
            event.preventDefault();

            // We have jQuery now... YAY, get on that.
            var $li = $PBJQ(event.target).closest('li');

            // when the tool menu is collapsed, a click should take you to the top page
            // and not toggle the menu
            if ($(document.body).is('.Mrphs-toolMenu-collapsed')) {
                location.href = topLevelPageHref;
                return false;
            }

            // the $menu expands the submenu
            // but collapse any other menus first
            $li.closest('ul').find('.expanded').each(function() {
                self.collapse($PBJQ(this));
            });

            self.expand($li);
        });

        $menu.addEventListener('keyup', function(event) {
            // We have jQuery now... YAY, get on that.
            var $li = $PBJQ(event.target).closest('li');

            if (event.keyCode == '13') {
                if (!$li.is('.expanded')) {
                    event.preventDefault();
                    event.stopImmediatePropagation();

                    self.expand($li, true, function() {
                        $collapseToggle.focus();
                    });

                    return false;
                }
            }

            return true;
        });

        $collapseToggle.addEventListener('click', function(event) {
            // We have jQuery now... YAY, get on that.
            var $li = $PBJQ(event.target).closest('li');

            self.collapse($li);
        });

        $collapseToggle.addEventListener('keyup', function(event) {
             // We have jQuery now... YAY, get on that.
             var $li = $PBJQ(event.target).closest('li');

             if (event.keyCode == '13') {
                 if ($li.is('.expanded')) {
                     event.preventDefault();
                     event.stopImmediatePropagation();

                     self.collapse($li, function() {
                         $menu.focus();
                     });

                     return false;
                 }
             }

             return true;
         });

        if ($li.classList.contains('is-current')) {
            $expandedMenuPlaceholder.style.display = 'block';
            $menu.style.display = 'none';

            $li.classList.add('expanded');
            var $submenu = $li.querySelector('.lessons-sub-page-menu');
            $submenu.style.display = 'block';
            $submenu.setAttribute('aria-hidden', false);
        }
    };


    LessonsSubPageNavigation.prototype.build_sub_page_url_for = function(sub_page) {
        var url = '/portal/site/' + sub_page.siteId;
        url += '/tool/' + sub_page.toolId;
        url += '/ShowPage?sendingPage='+sub_page.sendingPage;
        url += '&itemId='+sub_page.itemId;
        url += '&path=clear_and_push';
        url += '&title=' + sub_page.name;
        url += '&newTopLevel=false';
        return url;
    };

    LessonsSubPageNavigation.prototype.set_current_page_id = function(pageId) {
        if (this.has_current || pageId == this.currentPageId) {
            // if we already have a current set, then there's nothing to do
            return;
        }

        if (this.page_id_to_submenu_item[pageId]) {
            var li = this.page_id_to_submenu_item[pageId];
            // remove is-current from parent item
            var parent = li.parentElement.parentElement;
            parent.classList.add('is-parent-of-current');

            // add is-current to the submenu item
            li.classList.add('is-current');
        }
    };


    window.LessonsSubPageNavigation = LessonsSubPageNavigation;
})();
