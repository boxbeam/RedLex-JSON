package redempt.redlexjson;

import redempt.redlex.bnf.BNFParser;
import redempt.redlex.parser.Parser;
import redempt.redlex.processing.CullStrategy;
import redempt.redlex.processing.Lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static redempt.redlex.parser.ParserComponent.*;

public class RedLexJSON {
	
	private static Parser parser;
	
	private static synchronized Parser getParser() {
		if (parser == null) {
			Lexer lexer = BNFParser.createLexer(RedLexJSON.class.getResourceAsStream("/json.bnf"));
			lexer.setUnnamedRule(CullStrategy.LIFT_CHILDREN);
			lexer.setRetainStringLiterals(false);
			lexer.setRuleByName(CullStrategy.LIFT_CHILDREN, "sep", "object");
			
			parser = Parser.create(lexer,
				mapString("integer", Integer::parseInt),
				mapString("decimal", Double::parseDouble),
				mapString("boolean", Boolean::parseBoolean),
				mapString("escapeSequence", s -> {
					char c = s.charAt(1);
					return switch (c) {
						case 't' -> '\t';
						case 'n' -> '\n';
						default -> c;
					};
				}),
				mapString("strChar", s -> s.charAt(0)),
				mapChildren("string", c -> {
					StringBuilder builder = new StringBuilder();
					for (Object obj : c) {
						builder.append((char) obj);
					}
					return builder.toString();
				}),
				mapToken("null", t -> null),
				mapChildren("object", c -> c[0]),
				mapChildren("list", c -> {
					List<Object> list = new ArrayList<>();
					Collections.addAll(list, c);
					return list;
				}),
				mapChildren("mapEntry", c -> c),
				mapChildren("map", c -> {
					Map<String, Object> map = new HashMap<>();
					for (Object o : c) {
						Object[] pair = (Object[]) o;
						map.put((String) pair[0], pair[1]);
					}
					return map;
				})
			);
		}
		return parser;
	}
	
	public static Object parseJSON(String input) {
		return getParser().parse(input);
	}
	
}