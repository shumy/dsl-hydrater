grammar test.Grammar ;

Root: (Entity)+ ;

id@[StartWithUpperCase]
Entity: 'entity' & id -type-> text & '{'
  & fields -ref-> Field+ ';'
& '}' ;

Field: name -type-> text & ':' & type -ref-> Type & opt -exist-> '?' ;

Type: 'bool' | 'str' | 'int' | 'flt' | 'da' | 'tm' | 'dt' ;