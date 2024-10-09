lexer grammar SysYLexer;
CONST : 'const';

INT : 'int';

DOUBLE : 'double';

VOID : 'void';

IF : 'if';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';

IDENT : ('_' | [a-zA-Z])('_' | [a-zA-Z0-9])* ;


INTEGER_CONST : '0' [xX] [0-9a-fA-F]+ | '0' [0-7]* | [1-9] [0-9]*;


WS : [ \r\n\t]+ -> skip;

LINE_COMMENT : '//' .*?  '\n' -> skip;

MULTILINE_COMMENT : '/*' .*? '*/' -> skip;

