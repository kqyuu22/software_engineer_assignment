// package com.se.sebtl.util;

// import java.util.Set;
// import java.util.TreeSet;

// public class SlotIdParser {

//     public static Set<Integer> parse(String input) {
//         if (input == null || input.isBlank()) {
//             return Set.of();
//         }

//         Set<Integer> result = new TreeSet<>();
//         String[] parts = input.split(",");

//         for (String part : parts) {
//             String trimmed = part.trim();
//             if (trimmed.isEmpty()) {
//                 continue;
//             }

//             if (trimmed.contains("-")) {
//                 String[] range = trimmed.split("-", 2);
//                 int start = Integer.parseInt(range[0].trim());
//                 int end = Integer.parseInt(range[1].trim());
//                 if (start > end) {
//                     throw new IllegalArgumentException("Invalid range: start (" + start + ") must be <= end (" + end + ")");
//                 }
//                 for (int i = start; i <= end; i++) {
//                     result.add(i);
//                 }
//             } else {
//                 result.add(Integer.parseInt(trimmed));
//             }
//         }

//         return result;
//     }
// }
