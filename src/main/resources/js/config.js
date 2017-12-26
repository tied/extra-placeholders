define('com.mesilat.lov-placeholder:config',['jquery','ajs'],function($,AJS){
    function initDialog($dlg){
        $.ajax({
            url: AJS.contextPath() + '/rest/lov-resource/1.0/refdata',
            type: 'GET'
        }).done(function(data){
            $dlg.find('select[name="list-of-names"]').each(function(){
                var $select = $(this);
                $select.empty();
                data.forEach(function(rec){
                    $('<option>')
                        .attr('value', rec.code)
                        .text(rec.name)
                        .appendTo($select);
                });
                $('<option>')
                    .attr('value', '~~~ADD~NEW~~~')
                    .text(AJS.I18n.getText('com.mesilat.lov-placeholder.setting.addNew'))
                    .addClass('com-mesilat-autocomplete-select-dialog-add-new')
                    .appendTo($select);
            });
        }).fail(function(jqxhr){
            console.error('com.mesilat.lov-placeholder', jqxhr.responseText);
            AJS.flag({
                type: 'error',
                title: AJS.I18n.getText('com.mesilat.lov-placeholder.common.failure'),
                body: $('<p>').text(jqxhr.responseText).html(),
                close: 'auto'
            });
        });

        $dlg.find('select[name="list-of-names"]').each(function(){
            var $select = $(this);
            $select.on('change', function(e){
                $dlg.find('input[name="code"]').val('');
                $dlg.find('input[name="name"]').val('');
                $dlg.find('textarea').val('');
                var code = $(e.target).val();
                if (code !== '~~~ADD~NEW~~~'){
                    $.ajax({
                        url: AJS.contextPath() + '/rest/lov-resource/1.0/refdata/' + code,
                        type: 'GET'
                    }).done(function(data){
                        $dlg.find('input[name="code"]').val(data.code);
                        $dlg.find('input[name="name"]').val(data.name);
                        $dlg.find('textarea').val(data.data);
                    }).fail(function(jqxhr){
                        console.error('com.mesilat.lov-placeholder', jqxhr.responseText);
                        AJS.flag({
                            type: 'error',
                            title: AJS.I18n.getText('com.mesilat.lov-placeholder.common.failure'),
                            body: $('<p>').text(jqxhr.responseText).html(),
                            close: 'auto'
                        });
                    });
                }
            });
        });

        $dlg.find('input.com-mesilat-lov-placeholder-config-save').each(function(){
            var $save = $(this);
            $save.on('click', function(e){
                var data = {
                    code: $dlg.find('input[name="code"]').val(),
                    name: $dlg.find('input[name="name"]').val(),
                    data: $dlg.find('textarea').val()
                };
                var oldCode = $dlg.find('select[name="list-of-names"]').val();
                if (oldCode !== '~~~ADD~NEW~~~' && oldCode !== null){
                    data.oldCode = oldCode;
                }
                $.ajax({
                    url: AJS.contextPath() + '/rest/lov-resource/1.0/refdata',
                    type: 'POST',
                    dataType: 'json',
                    data: JSON.stringify(data),
                    contentType: 'application/json',
                    processData: false
                }).done(function(data){
                    var $select = $dlg.find('select[name="list-of-names"]');
                    if ('oldCode' in data){
                        var $option = $select.find('option[value="' + data.oldCode + '"]');
                        $option.attr('value', data.code);
                        $option.text(data.name);
                    } else {
                        $('<option>')
                            .attr('value', data.code)
                            .text(data.name)
                            .insertBefore($select.find('option[value="~~~ADD~NEW~~~"]'));
                        $select.val(data.code);
                    }

                    AJS.flag({
                        type: 'info',
                        title: AJS.I18n.getText('com.mesilat.lov-placeholder.common.success'),
                        body: $('<p>').text(AJS.I18n.getText('com.mesilat.lov-placeholder.setting.msg.data-saved')).html(),
                        close: 'auto'
                    });                    
                }).fail(function(jqxhr){
                    AJS.flag({
                        type: 'error',
                        title: AJS.I18n.getText('com.mesilat.lov-placeholder.common.failure'),
                        body: $('<p>').text(jqxhr.responseText).html(),
                        close: 'auto'
                    });
                });
            });
        });
        $dlg.find('.com-mesilat-lov-placeholder-config-delete').each(function(){
            var $delete = $(this);
            $delete.on('click', function(e){
                var id = $dlg.find('select[name="list-of-names"]').val();

                $.ajax({
                    url: AJS.contextPath() + '/rest/lov-resource/1.0/refdata/' + id,
                    type: 'DELETE'
                }).done(function(){
                    AJS.flag({
                        type: 'info',
                        title: AJS.I18n.getText('com.mesilat.lov-placeholder.common.success'),
                        body: $('<p>').text(AJS.I18n.getText('com.mesilat.lov-placeholder.setting.msg.data-deleted')).html(),
                        close: 'auto'
                    });                    
                    $dlg.find('select[name="list-of-names"] option[value="' + id + '"]').remove();
                }).fail(function(jqxhr){
                    AJS.flag({
                        type: 'error',
                        title: AJS.I18n.getText('com.mesilat.lov-placeholder.common.failure'),
                        body: $('<p>').text(jqxhr.responseText).html(),
                        close: 'auto'
                    });
                });
            });
        });
    }
    return {
        initDialog:initDialog
    };
});

(function($){
    $(function(){
        var config = require('com.mesilat.lov-placeholder:config');
        $('form.com-mesilat-lov-placeholder-config').each(function(){
            config.initDialog($(this));
        });
    });
   
})(AJS.$||$);