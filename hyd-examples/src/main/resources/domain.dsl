grammar test.Grammar ;

Root: (Entity)+ ;

id@[StartWithUpperCase]
Entity: 'entity' id=text '{'
  fields=Field+ ';'
'}' ;

Field: name=text ':' type=Type opt='?' ;

Type: 'bool' | 'str' | 'int' | 'flt' | 'da' | 'tm' | 'dt' ;