/**
 * Maps menu item name + category to the same stock photo filenames as the Android
 * FoodPhotoMapper.kt (drawable food_*.jpg copied to static/venue/food/).
 */
(function (global) {
  function words(name) {
    var s = new Set();
    String(name || '')
      .toLowerCase()
      .replace(/\w+/g, function (w) {
        s.add(w);
      });
    return s;
  }

  function categoryFallback(category) {
    switch (String(category || '')
      .trim()
      .toLowerCase()) {
      case 'starters':
        return 'food_bruschetta.jpg';
      case 'mains':
        return 'food_default_meal.jpg';
      case 'desserts':
        return 'food_cake.jpg';
      case 'drinks':
        return 'food_soft_drink.jpg';
      default:
        return 'food_default_meal.jpg';
    }
  }

  function foodPhotoBasename(name, category) {
    var lower = String(name || '').toLowerCase();
    var w = words(name);

    if (
      lower.indexOf('masala dosa') >= 0 ||
      lower.indexOf('rava dosa') >= 0 ||
      lower.indexOf('onion dosa') >= 0 ||
      lower.indexOf('uttapam') >= 0 ||
      lower.indexOf('uthappam') >= 0 ||
      lower.indexOf('pesarattu') >= 0 ||
      lower.indexOf('dosa') >= 0
    ) {
      return 'food_indian_dosa.jpg';
    }
    if (
      lower.indexOf('idli') >= 0 ||
      lower.indexOf('idiyappam') >= 0 ||
      lower.indexOf('medu vada') >= 0 ||
      (lower.indexOf('vada') >= 0 && lower.indexOf('vada pav') < 0)
    ) {
      return 'food_indian_idli.jpg';
    }
    if (
      lower.indexOf('vada pav') >= 0 ||
      lower.indexOf('vadapav') >= 0 ||
      lower.indexOf('pav bhaji') >= 0 ||
      lower.indexOf('pavbhaji') >= 0 ||
      lower.indexOf('misal') >= 0 ||
      lower.indexOf('misal pav') >= 0
    ) {
      return 'food_indian_gravy.jpg';
    }
    if (
      lower.indexOf('naan') >= 0 ||
      lower.indexOf('garlic naan') >= 0 ||
      lower.indexOf('butter naan') >= 0 ||
      lower.indexOf('kulcha') >= 0 ||
      lower.indexOf('roti') >= 0 ||
      lower.indexOf('chapati') >= 0 ||
      lower.indexOf('phulka') >= 0 ||
      lower.indexOf('paratha') >= 0 ||
      lower.indexOf('parantha') >= 0 ||
      lower.indexOf('aloo paratha') >= 0 ||
      lower.indexOf('bhatura') >= 0 ||
      lower.indexOf('bhature') >= 0 ||
      lower.indexOf('puri') >= 0 ||
      lower.indexOf('poori') >= 0 ||
      lower.indexOf('thepla') >= 0 ||
      lower.indexOf('missi roti') >= 0
    ) {
      return 'food_indian_flatbread.jpg';
    }
    if (lower.indexOf('haleem') >= 0) return 'food_indian_gravy.jpg';
    if (
      lower.indexOf('biryani') >= 0 ||
      lower.indexOf('biriyani') >= 0 ||
      lower.indexOf('hyderabadi') >= 0 ||
      lower.indexOf('lucknowi') >= 0 ||
      lower.indexOf('ambur') >= 0 ||
      lower.indexOf('donne') >= 0
    ) {
      return 'food_indian_biryani.jpg';
    }
    if (
      lower.indexOf('pulao') >= 0 ||
      lower.indexOf('pulav') >= 0 ||
      lower.indexOf('pilaf') >= 0 ||
      lower.indexOf('pilau') >= 0 ||
      lower.indexOf('jeera rice') >= 0 ||
      lower.indexOf('lemon rice') >= 0 ||
      lower.indexOf('curd rice') >= 0 ||
      lower.indexOf('tamarind rice') >= 0 ||
      lower.indexOf('tomato rice') >= 0
    ) {
      return 'food_indian_rice.jpg';
    }
    if (
      lower.indexOf('paneer') >= 0 ||
      lower.indexOf('palak paneer') >= 0 ||
      lower.indexOf('kadai paneer') >= 0 ||
      lower.indexOf('paneer tikka') >= 0 ||
      lower.indexOf('malai paneer') >= 0 ||
      lower.indexOf('shahi paneer') >= 0 ||
      lower.indexOf('matar paneer') >= 0
    ) {
      return 'food_indian_paneer.jpg';
    }
    if (
      lower.indexOf('dal ') >= 0 ||
      lower.indexOf('dal.') >= 0 ||
      lower.indexOf('daal') >= 0 ||
      lower.indexOf('dhal') >= 0 ||
      lower.indexOf('sambar') >= 0 ||
      lower.indexOf('sambhar') >= 0 ||
      lower.indexOf('rasam') >= 0 ||
      lower.indexOf('dal makhani') >= 0 ||
      lower.indexOf('dal tadka') >= 0 ||
      lower.indexOf('dal fry') >= 0 ||
      lower.startsWith('dal ')
    ) {
      return 'food_indian_dal.jpg';
    }
    if (
      lower.indexOf('chole') >= 0 ||
      lower.indexOf('chhole') >= 0 ||
      lower.indexOf('chana masala') >= 0 ||
      lower.indexOf('channa masala') >= 0 ||
      lower.indexOf('rajma') >= 0 ||
      lower.indexOf('kadhi') >= 0 ||
      lower.indexOf('undhiyu') >= 0 ||
      lower.indexOf('baingan') >= 0 ||
      lower.indexOf('bhindi') >= 0 ||
      lower.indexOf('aloo gobi') >= 0 ||
      lower.indexOf('aloo matar') >= 0 ||
      lower.indexOf('mixed veg') >= 0 ||
      lower.indexOf('vegetable curry') >= 0
    ) {
      return 'food_indian_gravy.jpg';
    }
    if (
      lower.indexOf('butter chicken') >= 0 ||
      lower.indexOf('chicken tikka masala') >= 0 ||
      lower.indexOf('tikka masala') >= 0 ||
      lower.indexOf('chicken curry') >= 0 ||
      lower.indexOf('mutton curry') >= 0 ||
      lower.indexOf('lamb curry') >= 0 ||
      lower.indexOf('fish curry') >= 0 ||
      lower.indexOf('prawn curry') >= 0 ||
      lower.indexOf('egg curry') >= 0 ||
      lower.indexOf('chettinad') >= 0 ||
      lower.indexOf('vindaloo') >= 0 ||
      lower.indexOf('rogan josh') >= 0 ||
      lower.indexOf('korma') >= 0 ||
      lower.indexOf('handi') >= 0 ||
      lower.indexOf('jalfrezi') >= 0 ||
      lower.indexOf('do pyaza') >= 0 ||
      lower.indexOf('saag') >= 0 ||
      lower.indexOf('malai kofta') >= 0 ||
      lower.indexOf('kofta') >= 0
    ) {
      return 'food_indian_gravy.jpg';
    }
    if (
      lower.indexOf('tandoori') >= 0 ||
      (lower.indexOf('tikka') >= 0 && (lower.indexOf('chicken') >= 0 || lower.indexOf('paneer') >= 0)) ||
      lower.indexOf('seekh kebab') >= 0 ||
      lower.indexOf('seekh kabab') >= 0 ||
      lower.indexOf('shami kebab') >= 0 ||
      lower.indexOf('reshmi kebab') >= 0 ||
      lower.indexOf('galouti') >= 0 ||
      (lower.indexOf('kebab') >= 0 && (lower.indexOf('indian') >= 0 || lower.indexOf('lucknow') >= 0))
    ) {
      return 'food_indian_tandoori.jpg';
    }
    if (
      lower.indexOf('samosa') >= 0 ||
      lower.indexOf('pakora') >= 0 ||
      lower.indexOf('pakoda') >= 0 ||
      lower.indexOf('bhaji') >= 0 ||
      lower.indexOf('bajji') >= 0 ||
      lower.indexOf('kachori') >= 0 ||
      lower.indexOf('kachauri') >= 0 ||
      lower.indexOf('mirchi bajji') >= 0 ||
      lower.indexOf('bhajiya') >= 0
    ) {
      return 'food_indian_snack_fried.jpg';
    }
    if (
      lower.indexOf('chaat') >= 0 ||
      lower.indexOf('bhel') >= 0 ||
      lower.indexOf('sev puri') >= 0 ||
      lower.indexOf('pani puri') >= 0 ||
      lower.indexOf('gol gappa') >= 0 ||
      lower.indexOf('puchka') >= 0 ||
      lower.indexOf('dahi puri') >= 0 ||
      lower.indexOf('papdi chaat') >= 0 ||
      lower.indexOf('aloo tikki') >= 0
    ) {
      return 'food_indian_snack_fried.jpg';
    }
    if (
      lower.indexOf('jalebi') >= 0 ||
      lower.indexOf('jalebee') >= 0 ||
      lower.indexOf('gulab jamun') >= 0 ||
      lower.indexOf('gulabjamun') >= 0 ||
      lower.indexOf('rasgulla') >= 0 ||
      lower.indexOf('ras malai') >= 0 ||
      lower.indexOf('rasmalai') >= 0 ||
      lower.indexOf('ladoo') >= 0 ||
      lower.indexOf('laddu') >= 0 ||
      lower.indexOf('barfi') >= 0 ||
      lower.indexOf('burfi') >= 0 ||
      lower.indexOf('halwa') >= 0 ||
      lower.indexOf('halva') >= 0 ||
      lower.indexOf('sheera') >= 0 ||
      lower.indexOf('shrikhand') >= 0 ||
      lower.indexOf('payasam') >= 0 ||
      lower.indexOf('kheer') >= 0 ||
      lower.indexOf('phirni') >= 0 ||
      lower.indexOf('kulfi') >= 0 ||
      lower.indexOf('mithai') >= 0 ||
      lower.indexOf('soan papdi') >= 0
    ) {
      return 'food_indian_sweet.jpg';
    }
    if (
      lower.indexOf('lassi') >= 0 ||
      lower.indexOf('chaas') >= 0 ||
      lower.indexOf('chaach') >= 0 ||
      (lower.indexOf('buttermilk') >= 0 && lower.indexOf('masala') >= 0)
    ) {
      return 'food_indian_lassi.jpg';
    }
    if (
      lower.indexOf('masala chai') >= 0 ||
      lower.indexOf('masala tea') >= 0 ||
      (lower.indexOf('chai') >= 0 && lower.indexOf('latte') < 0) ||
      lower.indexOf('cutting chai') >= 0 ||
      lower.indexOf('adrak chai') >= 0 ||
      lower.indexOf('ginger chai') >= 0
    ) {
      return 'food_indian_chai.jpg';
    }
    if (
      lower.indexOf('curry') >= 0 ||
      lower.indexOf('tikka') >= 0 ||
      lower.indexOf('masala') >= 0
    ) {
      return 'food_indian_curry.jpg';
    }
    if (lower.indexOf('ice cream') >= 0 || lower.indexOf('gelato') >= 0 || w.has('sundae')) return 'food_ice_cream.jpg';
    if (
      lower.indexOf('green tea') >= 0 ||
      lower.indexOf('black tea') >= 0 ||
      lower.indexOf('iced tea') >= 0 ||
      lower.indexOf('bubble tea') >= 0 ||
      lower.indexOf('matcha') >= 0 ||
      (w.has('tea') && lower.indexOf('chai') < 0)
    ) {
      return 'food_tea.jpg';
    }
    if (
      lower.indexOf('espresso') >= 0 ||
      lower.indexOf('latte') >= 0 ||
      lower.indexOf('cappuccino') >= 0 ||
      lower.indexOf('americano') >= 0 ||
      lower.indexOf('mocha') >= 0 ||
      lower.indexOf('macchiato') >= 0 ||
      lower.indexOf('frappuccino') >= 0 ||
      w.has('coffee')
    ) {
      return 'food_coffee.jpg';
    }
    if (lower.indexOf('margherita') >= 0 || w.has('pizza')) return 'food_pizza.jpg';
    if (lower.indexOf('caesar') >= 0 || w.has('salad')) return 'food_salad.jpg';
    if (lower.indexOf('soup') >= 0 || lower.indexOf('bisque') >= 0 || lower.indexOf('broth') >= 0) return 'food_soup.jpg';
    if (
      lower.indexOf('salmon') >= 0 ||
      lower.indexOf('seafood') >= 0 ||
      lower.indexOf('prawn') >= 0 ||
      lower.indexOf('shrimp') >= 0 ||
      lower.indexOf('tuna') >= 0 ||
      w.has('fish')
    ) {
      return 'food_seafood.jpg';
    }
    if (
      lower.indexOf('ribeye') >= 0 ||
      lower.indexOf('steak') >= 0 ||
      (w.has('beef') && lower.indexOf('burger') < 0)
    ) {
      return 'food_steak.jpg';
    }
    if (
      lower.indexOf('primavera') >= 0 ||
      lower.indexOf('spaghetti') >= 0 ||
      lower.indexOf('lasagna') >= 0 ||
      lower.indexOf('penne') >= 0 ||
      lower.indexOf('risotto') >= 0 ||
      w.has('pasta')
    ) {
      return 'food_pasta.jpg';
    }
    if (
      lower.indexOf('cheesecake') >= 0 ||
      lower.indexOf('brownie') >= 0 ||
      lower.indexOf('tiramisu') >= 0 ||
      lower.indexOf('pudding') >= 0 ||
      w.has('cake') ||
      w.has('dessert')
    ) {
      return 'food_cake.jpg';
    }
    if (
      lower.indexOf('wine') >= 0 ||
      lower.indexOf('prosecco') >= 0 ||
      lower.indexOf('champagne') >= 0 ||
      lower.indexOf('rosé') >= 0 ||
      lower.indexOf('rose wine') >= 0
    ) {
      return 'food_wine.jpg';
    }
    if (
      lower.indexOf('beer') >= 0 ||
      lower.indexOf('lager') >= 0 ||
      lower.indexOf('ipa') >= 0 ||
      lower.indexOf('stout') >= 0 ||
      lower.indexOf('ale') >= 0
    ) {
      return 'food_beer.jpg';
    }
    if (
      lower.indexOf('lime soda') >= 0 ||
      lower.indexOf('soda') >= 0 ||
      lower.indexOf('cola') >= 0 ||
      lower.indexOf('juice') >= 0 ||
      lower.indexOf('smoothie') >= 0 ||
      lower.indexOf('mocktail') >= 0 ||
      lower.indexOf('lemonade') >= 0
    ) {
      return 'food_soft_drink.jpg';
    }
    if (lower.indexOf('bruschetta') >= 0) return 'food_bruschetta.jpg';
    if (lower.indexOf('burger') >= 0 || lower.indexOf('cheeseburger') >= 0) return 'food_burger.jpg';
    if (lower.indexOf('sushi') >= 0 || lower.indexOf('sashimi') >= 0 || lower.indexOf('maki') >= 0) return 'food_sushi.jpg';
    if (
      lower.indexOf('ramen') >= 0 ||
      lower.indexOf('noodle') >= 0 ||
      lower.indexOf('pho') >= 0 ||
      lower.indexOf('pad thai') >= 0
    ) {
      return 'food_noodles.jpg';
    }
    return categoryFallback(category);
  }

  function foodPhotoUrlForMenuItem(name, category) {
    var base =
      typeof global.FOOD_STATIC_BASE !== 'undefined' && global.FOOD_STATIC_BASE
        ? global.FOOD_STATIC_BASE
        : '/static/venue/food/';
    if (base.slice(-1) !== '/') base += '/';
    return base + foodPhotoBasename(name, category);
  }

  function menuItemDisplayImageUrl(m) {
    if (!m) return foodPhotoUrlForMenuItem('', 'General');
    var u = String(m.image_url || m.custom_photo_url || '').trim();
    if (/^https?:\/\//i.test(u)) return u;
    if (u.indexOf('/media/') === 0) {
      return (global.location && global.location.origin ? global.location.origin : '') + u;
    }
    return foodPhotoUrlForMenuItem(m.name, m.category);
  }

  global.foodPhotoUrlForMenuItem = foodPhotoUrlForMenuItem;
  global.menuItemDisplayImageUrl = menuItemDisplayImageUrl;
})(typeof window !== 'undefined' ? window : this);
