-- Update the Facebook catalog template to match the composed FacebookTemplate
-- component tree introduced in PR #32/#33/#34 (FacebookTemplate, FacebookPostCard,
-- FacebookHeaderRow, FacebookHeaderLeft, FacebookMetaColumn, FacebookBodySection,
-- FacebookImageSection, FacebookFooterSection, FacebookActionButtonsRow).
--
-- The V6 seed only ever produced the old flat CanvasContainer/TextWidget tree, so
-- every study created from the "facebook" template (including brand new ones)
-- still rendered the pre-#32 layout: ResearcherEdit/ParticipationPage prioritize
-- saved feed.content over the component's own default Frame markup, so updating
-- the frontend components alone never changes what already-initialized studies
-- (or newly created ones, which copy this row) display.
--
-- FacebookTopBar/FacebookStories/FacebookCreatePost render fixed, prop-less
-- markup directly in FacebookTemplate's JSX (not via props.children), so they
-- are not represented as separate craft.js nodes here; only the actually
-- editable regions (post header, body, image, engagement bar, action labels)
-- are modeled, mirroring FacebookTemplate.tsx's node tree.
--
-- Existing study feed copies remain unchanged.
-- Version 2 identifies this composed FacebookTemplate document format.
UPDATE feed_templates
SET content = $facebook_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "FacebookTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "facebook-root": "facebook-root"
    }
  },
  "facebook-root": {
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
      "facebook-post"
    ],
    "linkedNodes": {}
  },
  "facebook-post": {
    "type": {
      "resolvedName": "FacebookPostCard"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Post Card",
    "custom": {},
    "parent": "facebook-root",
    "hidden": false,
    "nodes": [
      "facebook-header",
      "facebook-body",
      "facebook-image",
      "facebook-footer",
      "facebook-actions"
    ],
    "linkedNodes": {}
  },
  "facebook-header": {
    "type": {
      "resolvedName": "FacebookHeaderRow"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Header Row",
    "custom": {},
    "parent": "facebook-post",
    "hidden": false,
    "nodes": [
      "facebook-header-left",
      "eL5vN8Jq4T"
    ],
    "linkedNodes": {}
  },
  "facebook-header-left": {
    "type": {
      "resolvedName": "FacebookHeaderLeft"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Header Left",
    "custom": {},
    "parent": "facebook-header",
    "hidden": false,
    "nodes": [
      "a1B2c3D4Ef",
      "facebook-meta"
    ],
    "linkedNodes": {}
  },
  "a1B2c3D4Ef": {
    "type": {
      "resolvedName": "AvatarWidget"
    },
    "isCanvas": false,
    "props": {
      "src": "",
      "alt": "Emma Wilson",
      "size": 48,
      "styleId": "facebook"
    },
    "displayName": "Avatar Widget",
    "custom": {},
    "parent": "facebook-header-left",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "facebook-meta": {
    "type": {
      "resolvedName": "FacebookMetaColumn"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Meta Column",
    "custom": {},
    "parent": "facebook-header-left",
    "hidden": false,
    "nodes": [
      "nM3xQ7Lp2R",
      "tK9wZ1Yb6H"
    ],
    "linkedNodes": {}
  },
  "nM3xQ7Lp2R": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "Emma Wilson",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "facebook-meta",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "tK9wZ1Yb6H": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "2h ago · 🌐",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "facebook-meta",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "eL5vN8Jq4T": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "···",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "facebook-header",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "facebook-body": {
    "type": {
      "resolvedName": "FacebookBodySection"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Body Section",
    "custom": {},
    "parent": "facebook-post",
    "hidden": false,
    "nodes": [
      "bW2rT6Uo9C"
    ],
    "linkedNodes": {}
  },
  "bW2rT6Uo9C": {
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
    "parent": "facebook-body",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "facebook-image": {
    "type": {
      "resolvedName": "FacebookImageSection"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Image Section",
    "custom": {},
    "parent": "facebook-post",
    "hidden": false,
    "nodes": [
      "iP4sD7Ka1V"
    ],
    "linkedNodes": {}
  },
  "iP4sD7Ka1V": {
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
    "parent": "facebook-image",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "facebook-footer": {
    "type": {
      "resolvedName": "FacebookFooterSection"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Footer Section",
    "custom": {},
    "parent": "facebook-post",
    "hidden": false,
    "nodes": [
      "acX3mR8Fy5"
    ],
    "linkedNodes": {}
  },
  "acX3mR8Fy5": {
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
    "parent": "facebook-footer",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "facebook-actions": {
    "type": {
      "resolvedName": "FacebookActionButtonsRow"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Facebook Action Buttons Row",
    "custom": {},
    "parent": "facebook-post",
    "hidden": false,
    "nodes": [
      "lY6hB2Nc7Q",
      "cQ1jM9Wz3S",
      "sZ8fH4Tp6L"
    ],
    "linkedNodes": {}
  },
  "lY6hB2Nc7Q": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "👍 Like",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "facebook-actions",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "cQ1jM9Wz3S": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "💬 Comment",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "facebook-actions",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "sZ8fH4Tp6L": {
    "type": {
      "resolvedName": "TextWidget"
    },
    "isCanvas": false,
    "props": {
      "text": "↗ Share",
      "className": "",
      "styleId": "facebook"
    },
    "displayName": "Text Widget",
    "custom": {},
    "parent": "facebook-actions",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$facebook_template$::jsonb,
    schema_version = 2
WHERE code = 'facebook';
