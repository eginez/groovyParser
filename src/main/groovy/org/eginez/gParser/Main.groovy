package org.eginez.gParser

import java.io.BufferedReader
import java.io.InputStreamReader


import groovy.transform.CompileStatic
import groovy.transform.ToString
import static TokenType.*
import groovy.util.logging.Slf4j
import org.apache.log4j.BasicConfigurator

enum TokenType { AssignToken, NumberToken, SymbolToken, OperatorToken, LParToken, RParToken }

@ToString
class Token<T> {
    TokenType type
    T value
}

@ToString
class Node {
    Token<?> token = null
    List<Node> children = []
}

@CompileStatic
class Parser {

    Node parse(Queue<Token> tokens) throws Exception {
        expectExpression(tokens)
    }

    def Node expectExpression(Queue<Token> tokens) throws Exception {
        switch(tokens.peek().type){
            case SymbolToken: return expectAssigment(tokens)
            case NumberToken: return expectOperation(tokens)
            default: throw new IllegalStateException('Bad parsin expecting: symbol')
        }
    }
    
    Node expectAssigment(Queue<Token> tokens) throws Exception {
        Node identifier = expectSymbol(tokens.poll())
        Node assigment = expectOperator(tokens.poll(), AssignToken)
        Node operation = expectOperation(tokens)
        assigment.children += [identifier, operation]
        return assigment
    }

    Node expectOperation(Queue<Token> tokens) throws Exception {
        Node operand = expectOperand(tokens.poll())
        if(!tokens.isEmpty() && tokens.peek().type == OperatorToken) {
            Node optr = expectOperator(tokens.poll(), OperatorToken)
            Node opt = expectOperation(tokens)
            optr.children += [operand, opt]
            return optr
        } else {
            return operand
        }
    }

    Node expectOperand(Token t) throws Exception {
        if( t.type == SymbolToken || t.type == NumberToken) {
            return new Node(token: t)
        }
        throw new IllegalStateException('Bad parsing expecting: symbol or number')
    }

    Node expectSymbol(Token t) throws Exception {
        if(t.type != SymbolToken){
            throw new IllegalStateException('Bad parsing expecting: symbol')
        }
        return new Node(token: t, children: [])
    }

    Node expectOperator(Token t, TokenType type ) throws Exception {
        if (t.type != type) {
            throw new IllegalStateException("Bad parsing expecting: $type")
        }
        new Node(token: t, children: [])
    }
}

@CompileStatic
@Slf4j
/*
Grammar
expression : assigment | operation
assigment : identifier = operation
operation : operand (operator operand)*
operand: identifier | number
operator: +|-|'*'| \/
identifier: word
*/
class Main {

    Closure<String> takeWhile = { Queue q, Character f ->
        def bld = new StringBuilder()
        while(q.peek() != f && !q.isEmpty()) {
            bld.append(q.poll())
        }
        bld.toString()
    }

    def symbolTable = [:]

    public static void main(String[] args) {
        BasicConfigurator.configure();
        def main = new Main();
        main.start();
    }



    public void start() {
        BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
        
        while(true) {
            print('>')
            def queue = new LinkedList<Character>()
            queue.addAll(sc.readLine().toCharArray() as Character[])
            try {
                def tokens = proccessLine(queue)
                Node ast = parse(tokens)
                //log.debug ast.toString()
                println resolve(ast)
            } catch (Exception ex) {
                log.error('Bad parsing', ex)
            }
        }
    }

    def Node parse(Queue<Token> tokens) {
        def parser = new Parser()
        def ast = parser.parse(tokens)
        return ast
    }

    def resolve(Node node) {
        switch(node.token.type) {
            case SymbolToken:
                 if ((String) node.token.value in symbolTable) return symbolTable[node.token.value as String]
                 else throw new IllegalStateException("Not valid symbol ${node.token.value}")
            case NumberToken:
                return node.token.value
            case OperatorToken:
                int a = resolve(node.children[0]) as int
                int b = resolve(node.children[1]) as int
                switch(node.token.value){
                    case '+': return a + b
                    case '-': return a - b
                    case '*': return a * b
                    default: log.error('No recognized op');
                }
            case AssignToken:
                def name = node.children[0].token.value
                def value = resolve(node.children[1]) as int
                symbolTable[name as String] = value
                return value
           default: log.error('Unrecognized node'); break;
        }

    }


    def Queue<Token> proccessLine(Queue queue){
        def tokens = new LinkedList<Token>()
        while(!queue.isEmpty()){
            switch (queue.peek()) {
                case 'a'..'z': tokens << lexIdentifier(queue); break;
                case '0'..'9': tokens << lexNumber(queue); break;
                case ['+' , '*', '-', '/'].collect{it as char}: tokens << new Token<String>(type: TokenType.OperatorToken, value: queue.poll()); break;
                case ')': queue.poll(); tokens << new Token<String>(type: TokenType.RParToken, value: ')'); break;
                case '(': queue.poll(); tokens << new Token<String>(type: TokenType.LParToken, value: '('); break;
                case ' ': queue.poll(); break;
                case '=': queue.poll(); tokens << new Token<String>(type: TokenType.AssignToken, value: '='); break;
                default: println("Cant understant token: ${queue.poll()}"); System.exit(-1);
            }
        }
        tokens
    }

    def Token<String> lexIdentifier(Queue queue) {
        def value = takeWhile(queue, ' ' as char)
        new Token<String>(type: TokenType.SymbolToken, value: value)
    }

    def Token<Integer> lexNumber(Queue queue) {
        def value = takeWhile(queue, ' ' as char)
        new Token<Integer>(type: TokenType.NumberToken, value: value as Integer)
    }
}


