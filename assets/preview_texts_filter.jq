# Extract texts and subcategories from specific category titles (recursive)
# Usage: jq -f jq_filter.jq all_texts.json

def find_categories:
  if .title == "Cartas de Amor" or .title == "MENSAGEM" or .title == "O GUARDADOR DE REBANHOS" then
    {
      id: .id,
      title: .title,
      texts: .texts,
      subcategories: .subcategories
    }
  else
    empty
  end,
  (.subcategories[]? | find_categories);

[.[] | find_categories]
