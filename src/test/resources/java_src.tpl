package {{package}};

{{#import}}
import {{.}};
{{/import}}

public class {{class}} {

    /*
     * Copy constructor
     */
    public void {{class}}({{class}} src) {
        {{#properties}}
        {{setter}}(src.{{getter}}());
        {{/properties}}
    }

}