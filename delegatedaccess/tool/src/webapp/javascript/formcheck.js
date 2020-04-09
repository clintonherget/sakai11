$(function () {
    $(document).on('submit', 'form', function (e) {
        if ($(this).find("input[name='search']").length == 0) {
            // Not the form we're looking for.
            return;
        }

        $('.missing-term-warning').remove();

        var search = $(this).find("input[name='search']").val();
        var searchUser = $(this).find("input[name='instructorField']").val();
        var selectedTerm = $(this).find("tr:contains('Term')").find('select').val();

        if (!search && !searchUser && !selectedTerm) {
            $(this).prepend($('<div class="missing-term-warning alertMessage">Term is required if <b>Site</b> and <b>User</b> are blank</div>'));
            hideLoading();

            e.preventDefault();
            return false;
        }
    });

});
