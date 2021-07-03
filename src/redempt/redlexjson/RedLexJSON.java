package redempt.redlexjson;

import redempt.redlex.bnf.BNFParser;
import redempt.redlex.data.Token;
import redempt.redlex.processing.CullStrategy;
import redempt.redlex.processing.Lexer;
import redempt.redlex.processing.TokenFilter;
import redempt.redlex.processing.TraversalOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedLexJSON {

	private static Lexer lexer;
	
	private static Lexer getLexer() {
		if (lexer == null) {
			lexer = BNFParser.createLexer(RedLexJSON.class.getClassLoader().getResourceAsStream("json.bnf"));
		}
		return lexer;
	}
	
	public static Object parseJSON(String input) {
		Token token = getLexer().tokenize(input);
		token.cull(TokenFilter.removeUnnamed(CullStrategy.LIFT_CHILDREN),
				TokenFilter.removeStringLiterals(),
				TokenFilter.byName(CullStrategy.LIFT_CHILDREN, "sep", "object"));
		return parseJSON(token.getChildren()[0]);
	}
	
	private static Object parseJSON(Token token) {
		switch (token.getType().getName()) {
			case "integer":
				return Long.parseLong(token.getValue());
			case "decimal":
				return Double.parseDouble(token.getValue());
			case "boolean":
				return Boolean.parseBoolean(token.getValue());
			case "string":
				return processString(token);
			case "list":
				List<Object> list = new ArrayList<>();
				for (Token child : token.getChildren()) {
					list.add(parseJSON(child));
				}
				return list;
			case "map":
				Map<String, Object> map = new HashMap<>();
				for (Token child : token.getChildren()) {
					Token[] children = child.getChildren();
					map.put(processString(children[0]), parseJSON(children[1]));
				}
				return map;
			default:
				return null;
		}
	}
	
	private static String processString(Token string) {
		for (Token esc : string.allByName(TraversalOrder.SHALLOW, "escapeSequence")) {
			String value;
			char c = esc.getValue().charAt(1);
			value = switch (c) {
				case 'n' -> "\n";
				case 't' -> "\t";
				default -> "" + c;
			};
			;
			esc.setValue(value);
		}
		return string.joinChildren("");
	}
	
}
