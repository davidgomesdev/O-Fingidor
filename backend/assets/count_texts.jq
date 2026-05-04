def count_texts: (.texts | length) + ([.subcategories[]? | count_texts] | add // 0);
map(count_texts) | add
