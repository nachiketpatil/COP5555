package cop5555fa13;

import static cop5555fa13.TokenStream.Kind.AND;
import static cop5555fa13.TokenStream.Kind.ASSIGN;
import static cop5555fa13.TokenStream.Kind.COLON;
import static cop5555fa13.TokenStream.Kind.COMMA;
import static cop5555fa13.TokenStream.Kind.COMMENT;
import static cop5555fa13.TokenStream.Kind.DIV;
import static cop5555fa13.TokenStream.Kind.DOT;
import static cop5555fa13.TokenStream.Kind.EOF;
import static cop5555fa13.TokenStream.Kind.EQ;
import static cop5555fa13.TokenStream.Kind.GEQ;
import static cop5555fa13.TokenStream.Kind.GT;
import static cop5555fa13.TokenStream.Kind.IDENT;
import static cop5555fa13.TokenStream.Kind.INT_LIT;
import static cop5555fa13.TokenStream.Kind.LEQ;
import static cop5555fa13.TokenStream.Kind.LPAREN;
import static cop5555fa13.TokenStream.Kind.LSHIFT;
import static cop5555fa13.TokenStream.Kind.LSQUARE;
import static cop5555fa13.TokenStream.Kind.LT;
import static cop5555fa13.TokenStream.Kind.MINUS;
import static cop5555fa13.TokenStream.Kind.MOD;
import static cop5555fa13.TokenStream.Kind.NEQ;
import static cop5555fa13.TokenStream.Kind.NOT;
import static cop5555fa13.TokenStream.Kind.OR;
import static cop5555fa13.TokenStream.Kind.PLUS;
import static cop5555fa13.TokenStream.Kind.QUESTION;
import static cop5555fa13.TokenStream.Kind.RPAREN;
import static cop5555fa13.TokenStream.Kind.RSHIFT;
import static cop5555fa13.TokenStream.Kind.RSQUARE;
import static cop5555fa13.TokenStream.Kind.SEMI;
import static cop5555fa13.TokenStream.Kind.TIMES;
import static cop5555fa13.TokenStream.Kind.Z;
import static cop5555fa13.TokenStream.Kind._else;
import static cop5555fa13.TokenStream.Kind._if;
import static cop5555fa13.TokenStream.Kind._int;
import static cop5555fa13.TokenStream.Kind.image;
import static cop5555fa13.TokenStream.Kind.red;
import static cop5555fa13.TokenStream.Kind.x;
import static cop5555fa13.TokenStream.Kind.y;
import static cop5555fa13.TokenStream.Kind.*;

import java.util.Stack;

import cop5555fa13.TokenStream.Kind;
import cop5555fa13.TokenStream.LexicalException;
import cop5555fa13.TokenStream.Token;

/**
 * Scanner/ Lexer to separate tokens and classify them into token categories.
 * Scanner takes an instance of TokenStream class. This TokenStream contains the
 * code to be scanned in form of an array of characters. The code is read from
 * this array, and the tokens are sent back to TokenStream in form of List of
 * Tokens or Comments.
 * 
 * @author Nachiket Hiralal Patil (7130-4371)
 * 
 */
public class Scanner {

    public TokenStream stream;
    private int        position;

    /**
     * Constructor for Scanner. TokenStream instance which has the input code to
     * be scanned and has containers for output tokens.
     * 
     * @param stream
     *            [TokenStream]:
     */
    public Scanner(TokenStream stream) {
        this.stream = stream;
    }

    /**
     * Scan method that drives scan of the code.
     * 
     * @throws LexicalException
     *             : When an unknown letter or ineligible condition is
     *             encountered.
     */
    public void scan() throws LexicalException {
        position = 0;
        Token t;
        do {
            t = next();
            if (t.kind.equals(COMMENT)) {
                stream.comments.add((Token) t);
            } else
                stream.tokens.add(t);
        } while (!t.kind.equals(EOF));
    }

    /**
     * A method to get next token from the code. This method extracts next token
     * with separateToken() and classifies it with classifyToken().
     * 
     * @return A Token that is extracted and classified and ready to be sent to
     *         parser.
     * @throws LexicalException
     *             : When an unknown letter or ineligible condition is
     *             encountered.
     */
    private Token next() throws LexicalException {
        Token t = separateToken();
        return t;
    }

    /**
     * A method to separate tokens based on given lexical structure.
     * 
     * @return: A separated token ready to be classified.
     * @throws LexicalException
     *             : When an unknown letter or ineligible condition is
     *             encountered.
     */
    private Token separateToken() throws LexicalException {
        Kind kind = null;
        int beginPosition = -1;

        // ignore white-spaces except for EOF. Return a token of kind EOF with
        // EOF is encountered.
        try {
            while (Character.isWhitespace(stream.inputChars[position])) {
                if (stream.inputChars[position] == '\u001a') {
                    return stream.new Token(EOF, position, ++position);
                }
                ++position;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return stream.new Token(EOF, position, ++position);
        }

        switch (stream.inputChars[position]) {

            case '.': {
                kind = DOT;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case ';': {
                kind = SEMI;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case ',': {
                kind = COMMA;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '(': {
                kind = LPAREN;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case ')': {
                kind = RPAREN;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '[': {
                kind = LSQUARE;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case ']': {
                kind = RSQUARE;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '{': {
                kind = LBRACE;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '}': {
                kind = RBRACE;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case ':': {
                kind = COLON;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '?': {
                kind = QUESTION;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '|': {
                kind = OR;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '&': {
                kind = AND;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '+': {
                kind = PLUS;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '-': {
                kind = MINUS;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '*': {
                kind = TIMES;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }
            case '%': {
                kind = MOD;
                beginPosition = position;
                ++position;
                return stream.new Token(kind, beginPosition, position);
            }

            case '=': {
                beginPosition = position;
                ++position;
                if (position < stream.inputChars.length && stream.inputChars[position] == '=') {
                    kind = EQ;
                    ++position;
                    return stream.new Token(kind, beginPosition, position);
                } else {
                    kind = ASSIGN;
                    return stream.new Token(kind, beginPosition, position);
                }
            }

            case '!': {
                beginPosition = position;
                ++position;
                if (position < stream.inputChars.length && stream.inputChars[position] == '=') {
                    kind = NEQ;
                    ++position;
                    return stream.new Token(kind, beginPosition, position);
                } else {
                    kind = NOT;
                    return stream.new Token(kind, beginPosition, position);
                }
            }

            case '<': {
                beginPosition = position;
                ++position;
                if (position < stream.inputChars.length && stream.inputChars[position] == '=') {
                    kind = LEQ;
                    ++position;
                    return stream.new Token(kind, beginPosition, position);
                } else if (position < stream.inputChars.length && stream.inputChars[position] == '<') {
                    kind = LSHIFT;
                    ++position;
                    return stream.new Token(kind, beginPosition, position);
                } else {
                    kind = LT;
                    return stream.new Token(kind, beginPosition, position);
                }
            }

            case '>': {
                beginPosition = position;
                ++position;
                if (position < stream.inputChars.length && stream.inputChars[position] == '=') {
                    kind = GEQ;
                    ++position;
                    return stream.new Token(kind, beginPosition, position);
                } else if (position < stream.inputChars.length && stream.inputChars[position] == '>') {
                    kind = RSHIFT;
                    ++position;
                    return stream.new Token(kind, beginPosition, position);
                } else {
                    kind = GT;
                    return stream.new Token(kind, beginPosition, position);
                }
            }

            case '"': {
                beginPosition = position;
                ++position;
                kind = STRING_LIT;
                while (stream.inputChars[position] != '"') {
                    if (stream.inputChars[position] == '\\') {
                        ++position;
                    }
                    ++position;
                    if (position >= stream.inputChars.length) {
                        throw stream.new LexicalException(position,
                                "String literal is not properly closed with double quotes '\"'");
                    }
                }
                return stream.new Token(kind, beginPosition, ++position);
            }

            case '/': {
                beginPosition = position;
                ++position;
                if (position < stream.inputChars.length && stream.inputChars[position] == '/') {
                    kind = COMMENT;

                    while (!((stream.inputChars[position] == '\r' && stream.inputChars[++position] == '\n')
                            || stream.inputChars[position] == '\r'
                            || stream.inputChars[position] == '\u0085'
                            || stream.inputChars[position] == '\u2028'
                            || stream.inputChars[position] == '\u2029' || stream.inputChars[position] == '\u001a')) {
                        ++position;
                        if (position >= stream.inputChars.length) {
                            break;
                        }
                    }
                    return stream.new Token(kind, beginPosition, position);
                } else {
                    kind = DIV;
                    return stream.new Token(kind, beginPosition, position);
                }
            }

            default: {

                if (Character
                        .isJavaIdentifierStart(stream.inputChars[position])) {
                    kind = IDENT;
                    beginPosition = position;
                    ++position;
                    while (position < stream.inputChars.length
                            && Character
                                    .isJavaIdentifierPart(stream.inputChars[position])) {
                        ++position;
                        if (position >= stream.inputChars.length) {
                            break;
                        }
                    }

                    // classifying token for its kind based on the data in
                    // token.
                    String tokenValue = String.valueOf(stream.inputChars,
                            beginPosition, position - beginPosition);

                    switch (tokenValue) {

                        case "true":
                        case "false": {
                            kind = BOOLEAN_LIT;
                            break;
                        }
                        case "boolean": {
                            kind = _boolean;
                            break;
                        }
                        case "int": {
                            kind = _int;
                            break;
                        }
                        case "if": {
                            kind = _if;
                            break;
                        }
                        case "else": {
                            kind = _else;
                            break;
                        }
                        case "while": {
                            kind = _while;
                            break;
                        }
                        case "pause": {
                            kind = pause;
                            break;
                        }
                        case "x": {
                            kind = x;
                            break;
                        }
                        case "y": {
                            kind = y;
                            break;
                        }
                        case "Z": {
                            kind = Z;
                            break;
                        }
                        case "image": {
                            kind = image;
                            break;
                        }
                        case "pixel": {
                            kind = pixel;
                            break;
                        }
                        case "pixels": {
                            kind = pixels;
                            break;
                        }
                        case "red": {
                            kind = red;
                            break;
                        }
                        case "green": {
                            kind = green;
                            break;
                        }
                        case "blue": {
                            kind = blue;
                            break;
                        }
                        case "shape": {
                            kind = shape;
                            break;
                        }
                        case "width": {
                            kind = width;
                            break;
                        }
                        case "height": {
                            kind = height;
                            break;
                        }
                        case "location": {
                            kind = location;
                            break;
                        }
                        case "x_loc": {
                            kind = x_loc;
                            break;
                        }
                        case "y_loc": {
                            kind = y_loc;
                            break;
                        }
                        case "SCREEN_SIZE": {
                            kind = SCREEN_SIZE;
                            break;
                        }
                        case "visible": {
                            kind = visible;
                            break;
                        }
                    }

                    return stream.new Token(kind, beginPosition, position);
                } else if (Character.isDigit(stream.inputChars[position])) {
                    kind = INT_LIT;
                    beginPosition = position;
                    ++position;
                    while (Character.isDigit(stream.inputChars[position])) {
                        ++position;
                        if (position >= stream.inputChars.length) {
                            break;
                        }
                    }
                    return stream.new Token(kind, beginPosition, position);
                }

                throw stream.new LexicalException(position,
                        "Illegal character " + stream.inputChars[position]);
            }
        }
    }

    /**
     * Validation for the tokens separated from the code. Simple check like well
     * formedness of an expression is executed.
     * 
     * @throws LexicalException
     *             : If the expression in code is not well formed.
     */
    private void validateTokenStream() throws LexicalException {

        Stack<TokenStream.Token> validationBuffer = new Stack<TokenStream.Token>();
        for (Token t : stream.tokens) {

            if (t.kind == LBRACE || t.kind == LPAREN || t.kind == LSQUARE) {
                validationBuffer.push(t);
            } else if (t.kind == RBRACE) {
                final Token top = (TokenStream.Token) validationBuffer.pop();
                if (top.kind != LBRACE) {
                    throw stream.new LexicalException(t.beg - 1,
                            "Syntax Error. Illegal Expression.");
                }
            } else if (t.kind == RSQUARE) {
                final Token top = (TokenStream.Token) validationBuffer.pop();
                if (top.kind != LSQUARE) {
                    throw stream.new LexicalException(t.beg - 1,
                            "Syntax Error. Illegal Expression.");
                }
            } else if (t.kind == RPAREN) {
                final Token top = (TokenStream.Token) validationBuffer.pop();
                if (top.kind != LPAREN) {
                    throw stream.new LexicalException(t.beg - 1,
                            "Syntax Error. Illegal Expression.");
                }
            }
        }
    }
}
