package com.restaurant.management.ui.visual

import androidx.annotation.DrawableRes
import com.restaurant.management.R

/**
 * Bundled JPG photos (stock + Wikimedia Commons where noted).
 * Indian dishes use dedicated drawables (`food_indian_*`) so thumbnails match cuisine.
 */
private fun words(name: String): Set<String> =
    Regex("\\w+").findAll(name.lowercase()).map { it.value }.toSet()

@DrawableRes
fun foodPhotoForMenuItem(name: String, category: String): Int {
    val lower = name.lowercase()
    val w = words(name)

    return when {
        // --- Indian cuisine (match before generic Western overlaps) ---
        lower.contains("masala dosa") ||
            lower.contains("rava dosa") ||
            lower.contains("onion dosa") ||
            lower.contains("uttapam") ||
            lower.contains("uthappam") ||
            lower.contains("pesarattu") ||
            lower.contains("dosa") -> R.drawable.food_indian_dosa

        lower.contains("idli") ||
            lower.contains("idiyappam") ||
            lower.contains("medu vada") ||
            (lower.contains("vada") && !lower.contains("vada pav")) -> R.drawable.food_indian_idli

        lower.contains("vada pav") ||
            lower.contains("vadapav") ||
            lower.contains("pav bhaji") ||
            lower.contains("pavbhaji") ||
            lower.contains("misal") ||
            lower.contains("misal pav") -> R.drawable.food_indian_gravy

        lower.contains("naan") ||
            lower.contains("garlic naan") ||
            lower.contains("butter naan") ||
            lower.contains("kulcha") ||
            lower.contains("roti") ||
            lower.contains("chapati") ||
            lower.contains("phulka") ||
            lower.contains("paratha") ||
            lower.contains("parantha") ||
            lower.contains("aloo paratha") ||
            lower.contains("bhatura") ||
            lower.contains("bhature") ||
            lower.contains("puri") ||
            lower.contains("poori") ||
            lower.contains("thepla") ||
            lower.contains("missi roti") -> R.drawable.food_indian_flatbread

        lower.contains("haleem") -> R.drawable.food_indian_gravy

        lower.contains("biryani") ||
            lower.contains("biriyani") ||
            lower.contains("hyderabadi") ||
            lower.contains("lucknowi") ||
            lower.contains("ambur") ||
            lower.contains("donne") -> R.drawable.food_indian_biryani

        lower.contains("pulao") ||
            lower.contains("pulav") ||
            lower.contains("pilaf") ||
            lower.contains("pilau") ||
            lower.contains("jeera rice") ||
            lower.contains("lemon rice") ||
            lower.contains("curd rice") ||
            lower.contains("tamarind rice") ||
            lower.contains("tomato rice") -> R.drawable.food_indian_rice

        lower.contains("paneer") ||
            lower.contains("palak paneer") ||
            lower.contains("kadai paneer") ||
            lower.contains("paneer tikka") ||
            lower.contains("malai paneer") ||
            lower.contains("shahi paneer") ||
            lower.contains("matar paneer") -> R.drawable.food_indian_paneer

        lower.contains("dal ") ||
            lower.contains("dal.") ||
            lower.startsWith("dal ") ||
            lower.contains("daal") ||
            lower.contains("dhal") ||
            lower.contains("sambar") ||
            lower.contains("sambhar") ||
            lower.contains("rasam") ||
            lower.contains("dal makhani") ||
            lower.contains("dal tadka") ||
            lower.contains("dal fry") -> R.drawable.food_indian_dal

        lower.contains("chole") ||
            lower.contains("chhole") ||
            lower.contains("chana masala") ||
            lower.contains("channa masala") ||
            lower.contains("rajma") ||
            lower.contains("kadhi") ||
            lower.contains("undhiyu") ||
            lower.contains("baingan") ||
            lower.contains("bhindi") ||
            lower.contains("aloo gobi") ||
            lower.contains("aloo matar") ||
            lower.contains("mixed veg") ||
            lower.contains("vegetable curry") -> R.drawable.food_indian_gravy

        lower.contains("butter chicken") ||
            lower.contains("chicken tikka masala") ||
            lower.contains("tikka masala") ||
            lower.contains("chicken curry") ||
            lower.contains("mutton curry") ||
            lower.contains("lamb curry") ||
            lower.contains("fish curry") ||
            lower.contains("prawn curry") ||
            lower.contains("egg curry") ||
            lower.contains("chettinad") ||
            lower.contains("vindaloo") ||
            lower.contains("rogan josh") ||
            lower.contains("korma") ||
            lower.contains("handi") ||
            lower.contains("jalfrezi") ||
            lower.contains("do pyaza") ||
            lower.contains("saag") ||
            lower.contains("malai kofta") ||
            lower.contains("kofta") -> R.drawable.food_indian_gravy

        lower.contains("tandoori") ||
            (lower.contains("tikka") && (lower.contains("chicken") || lower.contains("paneer"))) ||
            lower.contains("seekh kebab") ||
            lower.contains("seekh kabab") ||
            lower.contains("shami kebab") ||
            lower.contains("reshmi kebab") ||
            lower.contains("galouti") ||
            lower.contains("kebab") && (lower.contains("indian") || lower.contains("lucknow")) -> R.drawable.food_indian_tandoori

        lower.contains("samosa") ||
            lower.contains("pakora") ||
            lower.contains("pakoda") ||
            lower.contains("bhaji") ||
            lower.contains("bajji") ||
            lower.contains("kachori") ||
            lower.contains("kachauri") ||
            lower.contains("mirchi bajji") ||
            lower.contains("bhajiya") -> R.drawable.food_indian_snack_fried

        lower.contains("chaat") ||
            lower.contains("bhel") ||
            lower.contains("sev puri") ||
            lower.contains("pani puri") ||
            lower.contains("gol gappa") ||
            lower.contains("puchka") ||
            lower.contains("dahi puri") ||
            lower.contains("papdi chaat") ||
            lower.contains("aloo tikki") -> R.drawable.food_indian_snack_fried

        lower.contains("jalebi") ||
            lower.contains("jalebee") ||
            lower.contains("gulab jamun") ||
            lower.contains("gulabjamun") ||
            lower.contains("rasgulla") ||
            lower.contains("ras malai") ||
            lower.contains("rasmalai") ||
            lower.contains("ladoo") ||
            lower.contains("laddu") ||
            lower.contains("barfi") ||
            lower.contains("burfi") ||
            lower.contains("halwa") ||
            lower.contains("halva") ||
            lower.contains("sheera") ||
            lower.contains("shrikhand") ||
            lower.contains("payasam") ||
            lower.contains("kheer") ||
            lower.contains("phirni") ||
            lower.contains("kulfi") ||
            lower.contains("mithai") ||
            lower.contains("soan papdi") -> R.drawable.food_indian_sweet

        lower.contains("lassi") ||
            lower.contains("chaas") ||
            lower.contains("chaach") ||
            lower.contains("buttermilk") && lower.contains("masala") -> R.drawable.food_indian_lassi

        lower.contains("masala chai") ||
            lower.contains("masala tea") ||
            (lower.contains("chai") && !lower.contains("latte")) ||
            lower.contains("cutting chai") ||
            lower.contains("adrak chai") ||
            lower.contains("ginger chai") -> R.drawable.food_indian_chai

        lower.contains("curry") ||
            lower.contains("biryani") ||
            lower.contains("tikka") ||
            lower.contains("masala") ||
            lower.contains("korma") -> R.drawable.food_indian_curry

        lower.contains("ice cream") || lower.contains("gelato") || w.contains("sundae") -> R.drawable.food_ice_cream

        lower.contains("green tea") ||
            lower.contains("black tea") ||
            lower.contains("iced tea") ||
            lower.contains("bubble tea") ||
            lower.contains("matcha") ||
            (w.contains("tea") && !lower.contains("chai")) -> R.drawable.food_tea

        lower.contains("espresso") ||
            lower.contains("latte") ||
            lower.contains("cappuccino") ||
            lower.contains("americano") ||
            lower.contains("mocha") ||
            lower.contains("macchiato") ||
            lower.contains("frappuccino") ||
            w.contains("coffee") -> R.drawable.food_coffee

        lower.contains("margherita") || w.contains("pizza") -> R.drawable.food_pizza

        lower.contains("caesar") || w.contains("salad") -> R.drawable.food_salad

        lower.contains("soup") || lower.contains("bisque") || lower.contains("broth") -> R.drawable.food_soup

        lower.contains("salmon") ||
            lower.contains("seafood") ||
            lower.contains("prawn") ||
            lower.contains("shrimp") ||
            lower.contains("tuna") ||
            w.contains("fish") -> R.drawable.food_seafood

        lower.contains("ribeye") ||
            lower.contains("steak") ||
            (w.contains("beef") && !lower.contains("burger")) -> R.drawable.food_steak

        lower.contains("primavera") ||
            lower.contains("spaghetti") ||
            lower.contains("lasagna") ||
            lower.contains("penne") ||
            lower.contains("risotto") ||
            w.contains("pasta") -> R.drawable.food_pasta

        lower.contains("cheesecake") ||
            lower.contains("brownie") ||
            lower.contains("tiramisu") ||
            lower.contains("pudding") ||
            w.contains("cake") ||
            w.contains("dessert") -> R.drawable.food_cake

        lower.contains("wine") ||
            lower.contains("prosecco") ||
            lower.contains("champagne") ||
            lower.contains("rosé") ||
            lower.contains("rose wine") -> R.drawable.food_wine

        lower.contains("beer") ||
            lower.contains("lager") ||
            lower.contains("ipa") ||
            lower.contains("stout") ||
            lower.contains("ale") -> R.drawable.food_beer

        lower.contains("lime soda") ||
            lower.contains("soda") ||
            lower.contains("cola") ||
            lower.contains("juice") ||
            lower.contains("smoothie") ||
            lower.contains("mocktail") ||
            lower.contains("lemonade") -> R.drawable.food_soft_drink

        lower.contains("bruschetta") -> R.drawable.food_bruschetta

        lower.contains("burger") || lower.contains("cheeseburger") -> R.drawable.food_burger

        lower.contains("sushi") ||
            lower.contains("sashimi") ||
            lower.contains("maki") -> R.drawable.food_sushi

        lower.contains("ramen") ||
            lower.contains("noodle") ||
            lower.contains("pho") ||
            lower.contains("pad thai") -> R.drawable.food_noodles

        else -> categoryFallback(category)
    }
}

@DrawableRes
private fun categoryFallback(category: String): Int =
    when (category.trim().lowercase()) {
        "starters" -> R.drawable.food_bruschetta
        "mains" -> R.drawable.food_default_meal
        "desserts" -> R.drawable.food_cake
        "drinks" -> R.drawable.food_soft_drink
        else -> R.drawable.food_default_meal
    }

@DrawableRes
fun foodPhotoForInventoryItem(name: String): Int {
    val lower = name.lowercase()
    val w = words(name)
    return when {
        w.contains("paneer") -> R.drawable.food_indian_paneer
        w.contains("besan") || w.contains("atta") || w.contains("maida") -> R.drawable.food_indian_flatbread
        w.contains("rice") || w.contains("basmati") -> R.drawable.food_indian_rice
        w.contains("dal") || w.contains("daal") || w.contains("toor") || w.contains("moong") || w.contains("masoor") ->
            R.drawable.food_indian_dal
        w.contains("mirchi") || w.contains("chilli") || w.contains("chili") -> R.drawable.food_tomato
        w.contains("ghee") -> R.drawable.food_olive_oil
        w.contains("tomato") || w.contains("tomatoes") -> R.drawable.food_tomato
        w.contains("chicken") -> R.drawable.food_chicken_raw
        lower.contains("olive") -> R.drawable.food_olive_oil
        lower.contains("wine") -> R.drawable.food_wine
        lower.contains("coffee") -> R.drawable.food_coffee_beans
        else -> R.drawable.food_default_meal
    }
}
