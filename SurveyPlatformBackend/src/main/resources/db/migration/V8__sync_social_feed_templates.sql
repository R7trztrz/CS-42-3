-- Synchronize catalog documents from the frontend template-json exports.
-- Existing study_feeds remain independent and are not modified.
-- Schema version 1 denotes the Craft.js node-map format, not the template revision.

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
      "facebook-root": "lEm8ZkjVKg"
    }
  },
  "lEm8ZkjVKg": {
    "type": {
      "resolvedName": "ResearcherEditCanvas"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Researcher Edit Canvas",
    "custom": {},
    "parent": "ROOT",
    "hidden": false,
    "nodes": [
      "3mg3HffB8l"
    ],
    "linkedNodes": {}
  },
  "3mg3HffB8l": {
    "type": {
      "resolvedName": "FacebookDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "facebook-shell"
    },
    "displayName": "Facebook Desktop Shell",
    "custom": {},
    "parent": "lEm8ZkjVKg",
    "hidden": false,
    "nodes": [
      "NyS7RFSZYh"
    ],
    "linkedNodes": {}
  },
  "NyS7RFSZYh": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full flex-col overflow-hidden bg-[#f0f2f5]"
    },
    "displayName": "div",
    "custom": {},
    "parent": "3mg3HffB8l",
    "hidden": false,
    "nodes": [
      "GVrIRKbW8q",
      "8enqmx05R0"
    ],
    "linkedNodes": {}
  },
  "GVrIRKbW8q": {
    "type": {
      "resolvedName": "FacebookTopBar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Top Bar",
    "custom": {},
    "parent": "NyS7RFSZYh",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "8enqmx05R0": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex min-h-0 flex-1 overflow-hidden"
    },
    "displayName": "div",
    "custom": {},
    "parent": "NyS7RFSZYh",
    "hidden": false,
    "nodes": [
      "WbxB0qd8fs",
      "qFPl3Rkggc",
      "BL62VtZ_fX"
    ],
    "linkedNodes": {}
  },
  "WbxB0qd8fs": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "w-[250px] shrink-0"
    },
    "displayName": "div",
    "custom": {},
    "parent": "8enqmx05R0",
    "hidden": false,
    "nodes": [
      "UUIvA03JT8"
    ],
    "linkedNodes": {}
  },
  "UUIvA03JT8": {
    "type": {
      "resolvedName": "FacebookLeftSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Left Sidebar",
    "custom": {},
    "parent": "WbxB0qd8fs",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "qFPl3Rkggc": {
    "type": {
      "resolvedName": "FacebookMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "facebook-main-feed"
    },
    "displayName": "Facebook Main Feed",
    "custom": {},
    "parent": "8enqmx05R0",
    "hidden": false,
    "nodes": [
      "sNgoc6u-rG",
      "lRIbDlz6XJ",
      "qsG_PWVhkg",
      "87buOFQaPn"
    ],
    "linkedNodes": {}
  },
  "sNgoc6u-rG": {
    "type": {
      "resolvedName": "FacebookStories"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Stories",
    "custom": {},
    "parent": "qFPl3Rkggc",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "lRIbDlz6XJ": {
    "type": {
      "resolvedName": "FacebookCreatePost"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Create Post",
    "custom": {},
    "parent": "qFPl3Rkggc",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "qsG_PWVhkg": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "facebook",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "qFPl3Rkggc",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "87buOFQaPn": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "facebook",
      "username": "@alexc",
      "displayName": "Alex Chen",
      "time": "4h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign",
      "likes": 31,
      "comments": 6,
      "shares": 3,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "qFPl3Rkggc",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "BL62VtZ_fX": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "w-[250px] shrink-0"
    },
    "displayName": "div",
    "custom": {},
    "parent": "8enqmx05R0",
    "hidden": false,
    "nodes": [
      "UzTW2-yAuo"
    ],
    "linkedNodes": {}
  },
  "UzTW2-yAuo": {
    "type": {
      "resolvedName": "FacebookRightSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Right Sidebar",
    "custom": {},
    "parent": "BL62VtZ_fX",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$facebook_template$::jsonb,
    schema_version = 1
WHERE code = 'facebook';

UPDATE feed_templates
SET content = $instagram_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "InstagramTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "instagram-root": "hsWxC1sAYD"
    }
  },
  "hsWxC1sAYD": {
    "type": {
      "resolvedName": "ResearcherEditCanvas"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Researcher Edit Canvas",
    "custom": {},
    "parent": "ROOT",
    "hidden": false,
    "nodes": [
      "YyE-MesIeM"
    ],
    "linkedNodes": {}
  },
  "YyE-MesIeM": {
    "type": {
      "resolvedName": "InstagramDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "instagram-shell"
    },
    "displayName": "Instagram Desktop Shell",
    "custom": {},
    "parent": "hsWxC1sAYD",
    "hidden": false,
    "nodes": [
      "JgB2w0J6qE"
    ],
    "linkedNodes": {}
  },
  "JgB2w0J6qE": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full overflow-hidden bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "YyE-MesIeM",
    "hidden": false,
    "nodes": [
      "NsqS1dxBc3",
      "UXbKCIdfFF",
      "lZkRNIQQMt"
    ],
    "linkedNodes": {}
  },
  "NsqS1dxBc3": {
    "type": {
      "resolvedName": "InstagramSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Sidebar",
    "custom": {},
    "parent": "JgB2w0J6qE",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "UXbKCIdfFF": {
    "type": {
      "resolvedName": "InstagramMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "instagram-main-feed"
    },
    "displayName": "Instagram Main Feed",
    "custom": {},
    "parent": "JgB2w0J6qE",
    "hidden": false,
    "nodes": [
      "9b5z1Pcrew"
    ],
    "linkedNodes": {}
  },
  "9b5z1Pcrew": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "mx-auto w-full max-w-[560px]"
    },
    "displayName": "div",
    "custom": {},
    "parent": "UXbKCIdfFF",
    "hidden": false,
    "nodes": [
      "RxGCXXGFkp",
      "Q42dir7f_O"
    ],
    "linkedNodes": {}
  },
  "RxGCXXGFkp": {
    "type": {
      "resolvedName": "InstagramStories"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Stories",
    "custom": {},
    "parent": "9b5z1Pcrew",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Q42dir7f_O": {
    "type": {
      "resolvedName": "InstagramPostList"
    },
    "isCanvas": true,
    "props": {
      "id": "instagram-post-list"
    },
    "displayName": "Instagram Post List",
    "custom": {},
    "parent": "9b5z1Pcrew",
    "hidden": false,
    "nodes": [
      "FGXx7i49Cl",
      "H33EAyVTGn"
    ],
    "linkedNodes": {}
  },
  "FGXx7i49Cl": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "instagram",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "Q42dir7f_O",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "H33EAyVTGn": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "instagram",
      "username": "@alexc",
      "displayName": "Alex Chen",
      "time": "4h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Another beautiful day around campus.",
      "likes": 23,
      "comments": 4,
      "shares": 2,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "Q42dir7f_O",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "lZkRNIQQMt": {
    "type": {
      "resolvedName": "InstagramSuggestions"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Suggestions",
    "custom": {},
    "parent": "JgB2w0J6qE",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$instagram_template$::jsonb,
    schema_version = 1
WHERE code = 'instagram';

UPDATE feed_templates
SET content = $tiktok_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "TikTokTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "TikTok Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "tiktok-root": "uXATnRg6Bd"
    }
  },
  "uXATnRg6Bd": {
    "type": {
      "resolvedName": "ResearcherEditCanvas"
    },
    "isCanvas": true,
    "props": {},
    "displayName": "Researcher Edit Canvas",
    "custom": {},
    "parent": "ROOT",
    "hidden": false,
    "nodes": [
      "Q4HsRCEO44"
    ],
    "linkedNodes": {}
  },
  "Q4HsRCEO44": {
    "type": {
      "resolvedName": "TikTokDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "tiktok-shell"
    },
    "displayName": "TikTok Desktop Shell",
    "custom": {},
    "parent": "uXATnRg6Bd",
    "hidden": false,
    "nodes": [
      "Ids1O4UavI"
    ],
    "linkedNodes": {}
  },
  "Ids1O4UavI": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full overflow-hidden"
    },
    "displayName": "div",
    "custom": {},
    "parent": "Q4HsRCEO44",
    "hidden": false,
    "nodes": [
      "ulgfrzj_n_",
      "gE1G4fbY43"
    ],
    "linkedNodes": {}
  },
  "ulgfrzj_n_": {
    "type": {
      "resolvedName": "TikTokSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "TikTok Sidebar",
    "custom": {},
    "parent": "Ids1O4UavI",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "gE1G4fbY43": {
    "type": {
      "resolvedName": "TikTokMainArea"
    },
    "isCanvas": true,
    "props": {
      "id": "tiktok-main"
    },
    "displayName": "TikTok Main Area",
    "custom": {},
    "parent": "Ids1O4UavI",
    "hidden": false,
    "nodes": [
      "bOw0-KxUdu",
      "eqVSJRKqT_"
    ],
    "linkedNodes": {}
  },
  "bOw0-KxUdu": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-[72px] shrink-0 items-center justify-center border-b border-[#1f1f1f] bg-[#0f0f0f]"
    },
    "displayName": "div",
    "custom": {},
    "parent": "gE1G4fbY43",
    "hidden": false,
    "nodes": [
      "JHVAGczY2j"
    ],
    "linkedNodes": {}
  },
  "JHVAGczY2j": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full items-center gap-9"
    },
    "displayName": "div",
    "custom": {},
    "parent": "bOw0-KxUdu",
    "hidden": false,
    "nodes": [
      "LLzcWktkiI",
      "AJoIpUJXj_"
    ],
    "linkedNodes": {}
  },
  "LLzcWktkiI": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "flex h-full items-center text-[14px] font-semibold",
      "style": {
        "color": "#777777"
      },
      "children": "Following"
    },
    "displayName": "span",
    "custom": {},
    "parent": "JHVAGczY2j",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "AJoIpUJXj_": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "flex h-full items-center border-b-2 border-white px-1 text-[14px] font-semibold",
      "style": {
        "color": "#ffffff"
      },
      "children": "For You"
    },
    "displayName": "span",
    "custom": {},
    "parent": "JHVAGczY2j",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "eqVSJRKqT_": {
    "type": {
      "resolvedName": "TikTokFeedStage"
    },
    "isCanvas": true,
    "props": {
      "id": "tiktok-feed"
    },
    "displayName": "TikTok Feed Stage",
    "custom": {},
    "parent": "gE1G4fbY43",
    "hidden": false,
    "nodes": [
      "26KY9hYu9T",
      "F6VbnWIZx4"
    ],
    "linkedNodes": {}
  },
  "26KY9hYu9T": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "tiktok",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "eqVSJRKqT_",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "F6VbnWIZx4": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "tiktok",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "eqVSJRKqT_",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$tiktok_template$::jsonb,
    schema_version = 1
WHERE code = 'tiktok';
