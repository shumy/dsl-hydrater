grammar test.Grammar ;

root: (Entity)+ ;

Entity: 'entity' & id -type-> text@StartWithUpperCase & '{'
  & fields -ref-> Field+ ';'
& '}' ;

Field: name -type-> text & ':' & type -ref-> Type & opt -exist-> '?' ;

Type: 'bool' | 'str' | 'int' | 'flt' | 'da' | 'tm' | 'dt' ;