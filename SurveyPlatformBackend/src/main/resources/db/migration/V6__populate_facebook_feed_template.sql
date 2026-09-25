-- Populate the Facebook catalog template for future study creation.
-- Existing study feed copies remain unchanged.
-- Version 1 identifies the initial Craft.js node-map document format.
UPDATE feed_templates
SET content = $facebook_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "CanvasContainer"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Canvas Container",
    "custom": {},
    "hidden": false,
    "nodes": [
      "F20jrKslzA",
      "2U0rK7f_C3"
    ],
    "linkedNodes": {}
  },
  "F20jrKslzA": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "What's on your mind?",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "ROOT",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "2U0rK7f_C3": {
    "type": {
      "resolvedName": "CanvasContainer"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Canvas Container",
    "custom": {},
    "parent": "ROOT",
    "hidden": false,
    "nodes": [
      "1iEwTyJG0G",
      "EwRpB2T-nv",
      "Px7lgTSDK_",
      "Pd3AtTre5w"
    ],
    "linkedNodes": {}
  },
  "1iEwTyJG0G": {
    "type": {
      "resolvedName": "AvatarWidget"
    },
    "isCanvas": false,
    "props": {
      "src": "",
      "alt": "Emma Wilson",
      "size": 40,
      "styleId": "facebook"
    },
    "displayName": "Avatar Widget",
    "custom": {},
    "parent": "2U0rK7f_C3",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "EwRpB2T-nv": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "2U0rK7f_C3",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Px7lgTSDK_": {
    "type": {
      "resolvedName": "ImageWidget"
    },
    "isCanvas": false,
    "props": {
      "src": "",
      "alt": "Campus autumn scenery",
      "styleId": "facebook"
    },
    "displayName": "Image Widget",
    "custom": {},
    "parent": "2U0rK7f_C3",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Pd3AtTre5w": {
    "type": {
      "resolvedName": "ActionBarWidget"
    },
    "isCanvas": false,
    "props": {
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "styleId": "facebook"
    },
    "displayName": "Action Bar Widget",
    "custom": {},
    "parent": "2U0rK7f_C3",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$facebook_template$::jsonb,
    schema_version = 1
WHERE code = 'facebook';
