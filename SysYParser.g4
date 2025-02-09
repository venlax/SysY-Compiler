parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;
}

program : compUnit;


compUnit : (funcDef | decl)+ EOF;



decl : constDecl | varDecl;


constDecl : CONST INT constDef (COMMA constDef)* SEMICOLON;



constDef : IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal;


constInitVal : constExp | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE;


varDecl : INT varDef (COMMA varDef)* SEMICOLON;

varDef : IDENT (L_BRACKT constExp R_BRACKT)* (ASSIGN initVal)?;


initVal : exp | L_BRACE (initVal (COMMA initVal)*)? R_BRACE;


funcDef : funcType IDENT L_PAREN (funcFParams)? R_PAREN block;


funcType : VOID | INT;


funcFParams : funcFParam (COMMA funcFParam)*;


funcFParam : INT IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?;


block : L_BRACE (blockItem)* R_BRACE;


blockItem : decl | stmt;


stmt : lVal ASSIGN exp SEMICOLON
    | (exp)? SEMICOLON
    | block
    | IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
    | WHILE L_PAREN cond R_PAREN stmt
    | BREAK SEMICOLON
    | CONTINUE SEMICOLON
    | RETURN (exp)? SEMICOLON
    ;

exp : L_PAREN exp R_PAREN
   | lVal
   | number
   | IDENT L_PAREN funcRParams? R_PAREN
   | unaryOp exp
   | exp (MUL | DIV | MOD) exp
   | exp (PLUS | MINUS) exp
   ;


cond : exp
   | cond (LT | GT | LE | GE) cond
   | cond (EQ | NEQ) cond
   | cond AND cond
   | cond OR cond
   ;

lVal : IDENT (L_BRACKT exp R_BRACKT)*;


number : INTEGER_CONST;



unaryOp : PLUS | MINUS | NOT;


funcRParams : param (COMMA param)*;


param : exp;


constExp : exp;

//exp : addExp;
//
//cond : lOrExp;
//
//lVal : IDENT (L_BRACKT exp L_BRACKT)* ;
//
//primaryExp : L_PAREN exp R_PAREN | lVal | INTEGER_CONST ;
//
//unaryExp : primaryExp | IDENT L_PAREN (funcRParams)? R_PAREN
//           | unaryOp unaryExp ;
//
//unaryOp : PLUS | MINUS | NOT ;
//
//funcRParams : exp (COMMA exp)* ;
//
//mulExp : unaryExp | mulExp (MUL | DIV | MOD) unaryExp ;
//
//addExp : mulExp | addExp (PLUS | MINUS) mulExp ;
//
//relExp : addExp | relExp (LT | GT | LE | GE) addExp ;
//
//eqExp : relExp | eqExp (EQ | NEQ) relExp ;
//
//lAndExp : eqExp | lAndExp AND eqExp  ;
//
//lOrExp : lAndExp | lOrExp OR  lAndExp ;
//
//constExp : addExp ;


















//
//

//prog : (varDecl | functionDecl)* EOF ;
//
//varDecl : type IDENT (ASSIGN expr)?  SEMICOLON;
//
//type : INT | DOUBLE | VOID ;
//
//functionDecl : type IDENT L_PAREN formalParameters? R_PAREN block ;
//
//formalParameters : formalParameter (COMMA formalParameter)* ;
//
//formalParameter : type IDENT ;
//
//block : L_BRACKT stat* R_BRACKT ;
//
//stat : block
//     | varDecl
//     | IF expr 'then' stat (ELSE stat)?
//     | RETURN expr? SEMICOLON
//     | expr ASSIGN expr SEMICOLON
//     | expr SEMICOLON
//     ;
//
//expr: IDENT L_PAREN exprList? R_PAREN
//    | expr L_BRACKT expr R_BRACKT
//    | op = MINUS expr
//    | op = NOT expr
//    | <assoc = right> expr '^' expr
//    | lhs = expr (op = MUL | op = DIV) rhs = expr
//    | lhs = expr (op = PLUS | op = MINUS) rhs = expr
//    | lhs = expr (op = EQ | op = NEQ) rhs = expr
//    | L_PAREN expr R_PAREN
//    | IDENT
//    | INT
//    ;
//exprList : expr (COMMA expr)* ;

