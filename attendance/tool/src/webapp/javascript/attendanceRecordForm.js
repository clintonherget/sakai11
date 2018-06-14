/*
 *  Copyright (c) 2017, University of Dayton
 *
 *  Licensed under the Educational Community License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *              http://opensource.org/licenses/ecl2
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

(function (attendance, $, undefined) {

    attendance.recordFormSetup = function() {

        // handle radio click
        $('#takeAttendanceTable').on('click', '.status-group-container :radio', function(event) {
            event.stopImmediatePropagation();

            var $radio = $(event.target);

            attendance.triggerAction({
                action: 'setStatus',
                recordid: $radio.data('recordid'),
                status: $radio.data('status'),
            }, function() {
                $radio.closest('tr').find('.active').removeClass('active');
                $radio.closest('div').addClass('active');
            });
        });

        // expand click zone
        $('#takeAttendanceTable').on('click', '.div-table-col', function(event) {
            $(this).find(':radio').trigger('click');
        });

        // handle comment toggle click
        $('#takeAttendanceTable').on('click', '.comment-container .commentToggle', function(event) {
            event.stopImmediatePropagation();

            var $toggle = $(event.target).closest('.commentToggle');

            attendance.triggerAction({
                action: 'viewComment',
                toggleid: $toggle.attr('id'),
                recordid: $toggle.closest('.comment-container').data('recordid'),
            }, function(status, data) {
              console.log(data);
            });
        });
    };


    attendance.actionCallbacks = {};
    attendance.nextRequestId = 0;

    attendance.ajaxComplete = function (requestId, status, data) {
        attendance.actionCallbacks[requestId](status, data);
        delete attendance.actionCallbacks[requestId];
    };

    attendance.triggerAction = function (params, callback) {
      params['_requestId'] = attendance.nextRequestId++;
      attendance.actionCallbacks[params['_requestId']] = callback || $.noop;
      $('#takeAttendanceTable').trigger('attendance.action', params);
    };

}(window.attendance = window.attendance || {}, jQuery ));
