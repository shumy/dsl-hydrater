grammar test.Grammar ;

Root: (Entity)+ ;

name@[StartWithUpperCase]
Entity: 'entity' name=id '{'
  fields=Field+ #';'
'}' ;

Field: name=text ':' type=Type opt='?'? ;

Type: 'bool' | 'str' | 'int' | 'flt' | 'da' | 'tm' | 'dt' ;