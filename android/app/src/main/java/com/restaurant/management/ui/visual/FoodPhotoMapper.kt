package com.restaurant.management.ui.visual

import androidx.annotation.DrawableRes
import com.restaurant.management.R

/**
 * Bundled JPG photos (Unsplash + Wikimedia Commons). Matches user-visible names so e.g. “Tea” → tea photo.
 */
private fun words(name: String): Set<String> =
    Regex("\\w+").findAll(name.lowercase()).map { it.value }.toSet()

@DrawableRes
fun foodPhotoForMenuItem(name: String, category: String): Int {
    val lower = name.lowercase()
    val w = words(name)

    return when {
        lower.contains("ice cream") || lower.contains("gelato") || w.contains("sundae") -> R.drawable.food_ice_cream

        lower.contains("green tea") ||
            lower.contains("black tea") ||
            lower.contains("iced tea") ||
            lower.contains("masala chai") ||
            lower.contains("chai") ||
            lower.contains("bubble tea") ||
            lower.contains("matcha") ||
            w.contains("tea") -> R.drawable.food_tea

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

        lower.contains("curry") ||
            lower.contains("biryani") ||
            lower.contains("tikka") ||
            lower.contains("masala") ||
            lower.contains("korma") -> R.drawable.food_curry

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
        w.contains("tomato") || w.contains("tomatoes") -> R.drawable.food_tomato
        w.contains("chicken") -> R.drawable.food_chicken_raw
        lower.contains("olive") -> R.drawable.food_olive_oil
        lower.contains("wine") -> R.drawable.food_wine
        lower.contains("coffee") -> R.drawable.food_coffee_beans
        else -> R.drawable.food_default_meal
    }
}
