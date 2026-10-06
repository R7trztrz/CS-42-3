-- Synchronize all seven platform templates from frontend template-json exports.
-- Preserve the blank template and independent study_feeds copies.
-- Schema version 1 identifies the Craft.js node-map format, not a template revision.

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
      "facebook-root": "-YnQVk4cM7"
    }
  },
  "-YnQVk4cM7": {
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
      "G3x5rJBfKW"
    ],
    "linkedNodes": {}
  },
  "G3x5rJBfKW": {
    "type": {
      "resolvedName": "FacebookDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "facebook-shell"
    },
    "displayName": "Facebook Desktop Shell",
    "custom": {},
    "parent": "-YnQVk4cM7",
    "hidden": false,
    "nodes": [
      "4ckWn5kcK3"
    ],
    "linkedNodes": {}
  },
  "4ckWn5kcK3": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full flex-col overflow-hidden bg-[#f0f2f5]"
    },
    "displayName": "div",
    "custom": {},
    "parent": "G3x5rJBfKW",
    "hidden": false,
    "nodes": [
      "A_FQCme0Uy",
      "HuliL9gF-7"
    ],
    "linkedNodes": {}
  },
  "A_FQCme0Uy": {
    "type": {
      "resolvedName": "FacebookTopBar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Top Bar",
    "custom": {},
    "parent": "4ckWn5kcK3",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "HuliL9gF-7": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex min-h-0 flex-1 overflow-hidden"
    },
    "displayName": "div",
    "custom": {},
    "parent": "4ckWn5kcK3",
    "hidden": false,
    "nodes": [
      "P-LzE-Kp7s",
      "QyLyJmqbqj",
      "OsBrHdO7d1"
    ],
    "linkedNodes": {}
  },
  "P-LzE-Kp7s": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "w-[250px] shrink-0"
    },
    "displayName": "div",
    "custom": {},
    "parent": "HuliL9gF-7",
    "hidden": false,
    "nodes": [
      "2ob7wOYOj_"
    ],
    "linkedNodes": {}
  },
  "2ob7wOYOj_": {
    "type": {
      "resolvedName": "FacebookLeftSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Left Sidebar",
    "custom": {},
    "parent": "P-LzE-Kp7s",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "QyLyJmqbqj": {
    "type": {
      "resolvedName": "FacebookMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "facebook-main-feed"
    },
    "displayName": "Facebook Main Feed",
    "custom": {},
    "parent": "HuliL9gF-7",
    "hidden": false,
    "nodes": [
      "SSbjs23yr0",
      "GLl50jy2Sc",
      "CrANmw9f0p",
      "vJ8QUi33_Z"
    ],
    "linkedNodes": {}
  },
  "SSbjs23yr0": {
    "type": {
      "resolvedName": "FacebookStories"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Stories",
    "custom": {},
    "parent": "QyLyJmqbqj",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "GLl50jy2Sc": {
    "type": {
      "resolvedName": "FacebookCreatePost"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Create Post",
    "custom": {},
    "parent": "QyLyJmqbqj",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "CrANmw9f0p": {
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
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "QyLyJmqbqj",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "vJ8QUi33_Z": {
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
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "QyLyJmqbqj",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "OsBrHdO7d1": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "w-[250px] shrink-0"
    },
    "displayName": "div",
    "custom": {},
    "parent": "HuliL9gF-7",
    "hidden": false,
    "nodes": [
      "LVajGQVOaT"
    ],
    "linkedNodes": {}
  },
  "LVajGQVOaT": {
    "type": {
      "resolvedName": "FacebookRightSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Facebook Right Sidebar",
    "custom": {},
    "parent": "OsBrHdO7d1",
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
      "instagram-root": "T0uNQLfJRK"
    }
  },
  "T0uNQLfJRK": {
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
      "2yw55rCWk8"
    ],
    "linkedNodes": {}
  },
  "2yw55rCWk8": {
    "type": {
      "resolvedName": "InstagramDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "instagram-shell"
    },
    "displayName": "Instagram Desktop Shell",
    "custom": {},
    "parent": "T0uNQLfJRK",
    "hidden": false,
    "nodes": [
      "c5DjRY9EwM"
    ],
    "linkedNodes": {}
  },
  "c5DjRY9EwM": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full overflow-hidden bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "2yw55rCWk8",
    "hidden": false,
    "nodes": [
      "a9XUSI5PiS",
      "kulL0rI68_",
      "ym8yiiZu-y"
    ],
    "linkedNodes": {}
  },
  "a9XUSI5PiS": {
    "type": {
      "resolvedName": "InstagramSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Sidebar",
    "custom": {},
    "parent": "c5DjRY9EwM",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "kulL0rI68_": {
    "type": {
      "resolvedName": "InstagramMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "instagram-main-feed"
    },
    "displayName": "Instagram Main Feed",
    "custom": {},
    "parent": "c5DjRY9EwM",
    "hidden": false,
    "nodes": [
      "Be0z7KeUJS"
    ],
    "linkedNodes": {}
  },
  "Be0z7KeUJS": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "mx-auto w-full max-w-[560px]"
    },
    "displayName": "div",
    "custom": {},
    "parent": "kulL0rI68_",
    "hidden": false,
    "nodes": [
      "QXqJeNDZEJ",
      "Q48f4EuO3e"
    ],
    "linkedNodes": {}
  },
  "QXqJeNDZEJ": {
    "type": {
      "resolvedName": "InstagramStories"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Stories",
    "custom": {},
    "parent": "Be0z7KeUJS",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Q48f4EuO3e": {
    "type": {
      "resolvedName": "InstagramPostList"
    },
    "isCanvas": true,
    "props": {
      "id": "instagram-post-list"
    },
    "displayName": "Instagram Post List",
    "custom": {},
    "parent": "Be0z7KeUJS",
    "hidden": false,
    "nodes": [
      "CSo4zwM-pO",
      "T1hcZim8PL"
    ],
    "linkedNodes": {}
  },
  "CSo4zwM-pO": {
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
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "Q48f4EuO3e",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "T1hcZim8PL": {
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
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "Q48f4EuO3e",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "ym8yiiZu-y": {
    "type": {
      "resolvedName": "InstagramSuggestions"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Instagram Suggestions",
    "custom": {},
    "parent": "c5DjRY9EwM",
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
      "tiktok-root": "CpeqsM2xdb"
    }
  },
  "CpeqsM2xdb": {
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
      "H_PhMNArYx"
    ],
    "linkedNodes": {}
  },
  "H_PhMNArYx": {
    "type": {
      "resolvedName": "TikTokDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "tiktok-shell"
    },
    "displayName": "TikTok Desktop Shell",
    "custom": {},
    "parent": "CpeqsM2xdb",
    "hidden": false,
    "nodes": [
      "uRoIm70Ei6"
    ],
    "linkedNodes": {}
  },
  "uRoIm70Ei6": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full overflow-hidden"
    },
    "displayName": "div",
    "custom": {},
    "parent": "H_PhMNArYx",
    "hidden": false,
    "nodes": [
      "8YjQrA4dOo",
      "9n87GwkMMl"
    ],
    "linkedNodes": {}
  },
  "8YjQrA4dOo": {
    "type": {
      "resolvedName": "TikTokSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "TikTok Sidebar",
    "custom": {},
    "parent": "uRoIm70Ei6",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "9n87GwkMMl": {
    "type": {
      "resolvedName": "TikTokMainArea"
    },
    "isCanvas": true,
    "props": {
      "id": "tiktok-main"
    },
    "displayName": "TikTok Main Area",
    "custom": {},
    "parent": "uRoIm70Ei6",
    "hidden": false,
    "nodes": [
      "ffRkBguY4K",
      "R8weLwtmql"
    ],
    "linkedNodes": {}
  },
  "ffRkBguY4K": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-[72px] shrink-0 items-center justify-center border-b border-[#1f1f1f] bg-[#0f0f0f]"
    },
    "displayName": "div",
    "custom": {},
    "parent": "9n87GwkMMl",
    "hidden": false,
    "nodes": [
      "f3uJneO-ib"
    ],
    "linkedNodes": {}
  },
  "f3uJneO-ib": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full items-center gap-9"
    },
    "displayName": "div",
    "custom": {},
    "parent": "ffRkBguY4K",
    "hidden": false,
    "nodes": [
      "tNRc_xb7lC",
      "ANiLI2OVy3"
    ],
    "linkedNodes": {}
  },
  "tNRc_xb7lC": {
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
    "parent": "f3uJneO-ib",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "ANiLI2OVy3": {
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
    "parent": "f3uJneO-ib",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "R8weLwtmql": {
    "type": {
      "resolvedName": "TikTokFeedStage"
    },
    "isCanvas": true,
    "props": {
      "id": "tiktok-feed"
    },
    "displayName": "TikTok Feed Stage",
    "custom": {},
    "parent": "9n87GwkMMl",
    "hidden": false,
    "nodes": [
      "9MheN2Zken"
    ],
    "linkedNodes": {}
  },
  "9MheN2Zken": {
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
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "R8weLwtmql",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$tiktok_template$::jsonb,
    schema_version = 1
WHERE code = 'tiktok';

UPDATE feed_templates
SET content = $x_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "XTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "X Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "x-root": "BQC9-UA3PL"
    }
  },
  "BQC9-UA3PL": {
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
      "n62cTFwvhX"
    ],
    "linkedNodes": {}
  },
  "n62cTFwvhX": {
    "type": {
      "resolvedName": "XDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "x-shell"
    },
    "displayName": "X Desktop Shell",
    "custom": {},
    "parent": "BQC9-UA3PL",
    "hidden": false,
    "nodes": [
      "34atsQRlzG"
    ],
    "linkedNodes": {}
  },
  "34atsQRlzG": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full bg-black text-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "n62cTFwvhX",
    "hidden": false,
    "nodes": [
      "j3KxLYeZBh",
      "Kqppd2oVIW",
      "Bu0loBEm2z"
    ],
    "linkedNodes": {}
  },
  "j3KxLYeZBh": {
    "type": {
      "resolvedName": "XSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "X Sidebar",
    "custom": {},
    "parent": "34atsQRlzG",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Kqppd2oVIW": {
    "type": {
      "resolvedName": "XMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "x-main-feed"
    },
    "displayName": "X Main Feed",
    "custom": {},
    "parent": "34atsQRlzG",
    "hidden": false,
    "nodes": [
      "s8k30TXc4e",
      "zD8Kn-CsON",
      "-laxhZ9R3L"
    ],
    "linkedNodes": {}
  },
  "s8k30TXc4e": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "sticky top-0 z-20 grid h-[64px] shrink-0 grid-cols-2 border-b border-[#2f3336] bg-black"
    },
    "displayName": "div",
    "custom": {},
    "parent": "Kqppd2oVIW",
    "hidden": false,
    "nodes": [
      "LIQKJgWrNz",
      "4WlDagtVM6"
    ],
    "linkedNodes": {}
  },
  "LIQKJgWrNz": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "relative flex h-full items-center justify-center"
    },
    "displayName": "div",
    "custom": {},
    "parent": "s8k30TXc4e",
    "hidden": false,
    "nodes": [
      "EwlndzZkqZ",
      "KCRvuw3W3H"
    ],
    "linkedNodes": {}
  },
  "EwlndzZkqZ": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "text-[15px] font-semibold text-[#e7e9ea]",
      "children": "For You"
    },
    "displayName": "span",
    "custom": {},
    "parent": "LIQKJgWrNz",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "KCRvuw3W3H": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "absolute bottom-0 h-[4px] w-[64px] rounded-full bg-[#1d9bf0]"
    },
    "displayName": "span",
    "custom": {},
    "parent": "LIQKJgWrNz",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "4WlDagtVM6": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full items-center justify-center"
    },
    "displayName": "div",
    "custom": {},
    "parent": "s8k30TXc4e",
    "hidden": false,
    "nodes": [
      "BeDNkPIbBE"
    ],
    "linkedNodes": {}
  },
  "BeDNkPIbBE": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "text-[15px] font-medium text-[#71767b]",
      "children": "Following"
    },
    "displayName": "span",
    "custom": {},
    "parent": "4WlDagtVM6",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "zD8Kn-CsON": {
    "type": {
      "resolvedName": "XComposer"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "X Composer",
    "custom": {},
    "parent": "Kqppd2oVIW",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "-laxhZ9R3L": {
    "type": {
      "resolvedName": "XPostList"
    },
    "isCanvas": true,
    "props": {
      "id": "x-post-list"
    },
    "displayName": "X Post List",
    "custom": {},
    "parent": "Kqppd2oVIW",
    "hidden": false,
    "nodes": [
      "th2lWn97Hz",
      "g5AFiS3TLU"
    ],
    "linkedNodes": {}
  },
  "th2lWn97Hz": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "x",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "-laxhZ9R3L",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "g5AFiS3TLU": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "x",
      "username": "@alexc",
      "displayName": "Alex Chen",
      "time": "4h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign",
      "likes": 83,
      "comments": 21,
      "shares": 34,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "-laxhZ9R3L",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Bu0loBEm2z": {
    "type": {
      "resolvedName": "XRightSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "X Right Sidebar",
    "custom": {},
    "parent": "34atsQRlzG",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$x_template$::jsonb,
    schema_version = 1
WHERE code = 'x';

UPDATE feed_templates
SET content = $threads_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "ThreadsTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Threads Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "threads-root": "LJi4PidAQy"
    }
  },
  "LJi4PidAQy": {
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
      "OwKR-LKqHY"
    ],
    "linkedNodes": {}
  },
  "OwKR-LKqHY": {
    "type": {
      "resolvedName": "ThreadsDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "threads-shell"
    },
    "displayName": "Threads Desktop Shell",
    "custom": {},
    "parent": "LJi4PidAQy",
    "hidden": false,
    "nodes": [
      "udyvqlzPA8"
    ],
    "linkedNodes": {}
  },
  "udyvqlzPA8": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "OwKR-LKqHY",
    "hidden": false,
    "nodes": [
      "fehchd8ffa",
      "GuXq5DEkc6"
    ],
    "linkedNodes": {}
  },
  "fehchd8ffa": {
    "type": {
      "resolvedName": "ThreadsSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Threads Sidebar",
    "custom": {},
    "parent": "udyvqlzPA8",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "GuXq5DEkc6": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex min-w-0 flex-1 justify-center"
    },
    "displayName": "div",
    "custom": {},
    "parent": "udyvqlzPA8",
    "hidden": false,
    "nodes": [
      "gvNFDxfUrf"
    ],
    "linkedNodes": {}
  },
  "gvNFDxfUrf": {
    "type": {
      "resolvedName": "ThreadsMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "threads-main-feed"
    },
    "displayName": "Threads Main Feed",
    "custom": {},
    "parent": "GuXq5DEkc6",
    "hidden": false,
    "nodes": [
      "i_0355EvyM",
      "WvPVn43g2j",
      "OJ4SXczKCz"
    ],
    "linkedNodes": {}
  },
  "i_0355EvyM": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "sticky top-0 z-20 grid h-[62px] grid-cols-2 border-b border-[#eeeeee] bg-white/95 backdrop-blur"
    },
    "displayName": "div",
    "custom": {},
    "parent": "gvNFDxfUrf",
    "hidden": false,
    "nodes": [
      "i-SIOXUmCQ",
      "tiA0xSLTA2"
    ],
    "linkedNodes": {}
  },
  "i-SIOXUmCQ": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "relative flex items-center justify-center"
    },
    "displayName": "div",
    "custom": {},
    "parent": "i_0355EvyM",
    "hidden": false,
    "nodes": [
      "Yy4rgrEXeh",
      "evIYGb47Kb"
    ],
    "linkedNodes": {}
  },
  "Yy4rgrEXeh": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "text-[15px] font-semibold text-[#111111]",
      "children": "For you"
    },
    "displayName": "span",
    "custom": {},
    "parent": "i-SIOXUmCQ",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "evIYGb47Kb": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "absolute bottom-0 h-[3px] w-[54px] rounded-full bg-[#111111]"
    },
    "displayName": "span",
    "custom": {},
    "parent": "i-SIOXUmCQ",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "tiA0xSLTA2": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex items-center justify-center text-[15px] font-medium text-[#9ca3af]",
      "children": "Following"
    },
    "displayName": "div",
    "custom": {},
    "parent": "i_0355EvyM",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "WvPVn43g2j": {
    "type": {
      "resolvedName": "ThreadsComposer"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Threads Composer",
    "custom": {},
    "parent": "gvNFDxfUrf",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "OJ4SXczKCz": {
    "type": {
      "resolvedName": "ThreadsPostList"
    },
    "isCanvas": true,
    "props": {
      "id": "threads-post-list"
    },
    "displayName": "Threads Post List",
    "custom": {},
    "parent": "gvNFDxfUrf",
    "hidden": false,
    "nodes": [
      "lIefum0fB-",
      "5Ddp288D5E",
      "puPFbsthMB"
    ],
    "linkedNodes": {}
  },
  "lIefum0fB-": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "threads",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "OJ4SXczKCz",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "5Ddp288D5E": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "threads",
      "username": "@alexc",
      "displayName": "Alex Chen",
      "time": "4h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign",
      "likes": 83,
      "comments": 21,
      "shares": 34,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "OJ4SXczKCz",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "puPFbsthMB": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "threads",
      "username": "@jordanl",
      "displayName": "Jordan Lee",
      "time": "6h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Tried the new coffee shop near the university — incredible atmosphere for deep work. Highly recommend to fellow students! ☕",
      "likes": 62,
      "comments": 9,
      "shares": 5,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "OJ4SXczKCz",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$threads_template$::jsonb,
    schema_version = 1
WHERE code = 'threads';

UPDATE feed_templates
SET content = $bluesky_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "BlueskyTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Bluesky Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "bluesky-root": "QZHMs3fzbO"
    }
  },
  "QZHMs3fzbO": {
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
      "vgrjKzcHhd"
    ],
    "linkedNodes": {}
  },
  "vgrjKzcHhd": {
    "type": {
      "resolvedName": "BlueskyDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "bluesky-shell"
    },
    "displayName": "Bluesky Desktop Shell",
    "custom": {},
    "parent": "QZHMs3fzbO",
    "hidden": false,
    "nodes": [
      "cFsapr-U_a"
    ],
    "linkedNodes": {}
  },
  "cFsapr-U_a": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "vgrjKzcHhd",
    "hidden": false,
    "nodes": [
      "sc_j9x5OAD",
      "Y7dzL7aE53",
      "E9L8AG2T3L",
      "Rc3Wfa8DW6"
    ],
    "linkedNodes": {}
  },
  "sc_j9x5OAD": {
    "type": {
      "resolvedName": "BlueskySidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Bluesky Sidebar",
    "custom": {},
    "parent": "cFsapr-U_a",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Y7dzL7aE53": {
    "type": {
      "resolvedName": "BlueskyMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "bluesky-main-feed"
    },
    "displayName": "Bluesky Main Feed",
    "custom": {},
    "parent": "cFsapr-U_a",
    "hidden": false,
    "nodes": [
      "yk6MEqVzWX",
      "XTf0htI30n",
      "pGckdh6Eqp"
    ],
    "linkedNodes": {}
  },
  "yk6MEqVzWX": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "sticky top-0 z-20 grid h-[58px] grid-cols-2 border-b border-[#e6edf5] bg-white/95 backdrop-blur"
    },
    "displayName": "div",
    "custom": {},
    "parent": "Y7dzL7aE53",
    "hidden": false,
    "nodes": [
      "i9UxXIwKq9",
      "TQWg6sb61H"
    ],
    "linkedNodes": {}
  },
  "i9UxXIwKq9": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "relative flex items-center justify-center"
    },
    "displayName": "div",
    "custom": {},
    "parent": "yk6MEqVzWX",
    "hidden": false,
    "nodes": [
      "tryknTWO8O",
      "ChVqnpTaoc"
    ],
    "linkedNodes": {}
  },
  "tryknTWO8O": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "text-[14px] font-semibold text-[#1185fe]",
      "children": "Following"
    },
    "displayName": "span",
    "custom": {},
    "parent": "i9UxXIwKq9",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "ChVqnpTaoc": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "absolute bottom-0 h-[3px] w-[48px] rounded-full bg-[#1185fe]"
    },
    "displayName": "span",
    "custom": {},
    "parent": "i9UxXIwKq9",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "TQWg6sb61H": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex items-center justify-center text-[14px] font-semibold text-[#64748b]",
      "children": "Discover"
    },
    "displayName": "div",
    "custom": {},
    "parent": "yk6MEqVzWX",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "XTf0htI30n": {
    "type": {
      "resolvedName": "BlueskyComposer"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Bluesky Composer",
    "custom": {},
    "parent": "Y7dzL7aE53",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "pGckdh6Eqp": {
    "type": {
      "resolvedName": "BlueskyPostList"
    },
    "isCanvas": true,
    "props": {
      "id": "bluesky-post-list"
    },
    "displayName": "Bluesky Post List",
    "custom": {},
    "parent": "Y7dzL7aE53",
    "hidden": false,
    "nodes": [
      "GGeJc_E7Qy",
      "sDVn4LCYm7"
    ],
    "linkedNodes": {}
  },
  "GGeJc_E7Qy": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "bluesky",
      "username": "@emmaw.bsky.social",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "pGckdh6Eqp",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "sDVn4LCYm7": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "bluesky",
      "username": "@alexc.bsky.social",
      "displayName": "Alex Chen",
      "time": "4h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign",
      "likes": 83,
      "comments": 21,
      "shares": 34,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "pGckdh6Eqp",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "E9L8AG2T3L": {
    "type": {
      "resolvedName": "BlueskyRightSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Bluesky Right Sidebar",
    "custom": {},
    "parent": "cFsapr-U_a",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Rc3Wfa8DW6": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "min-w-0 flex-1 bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "cFsapr-U_a",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$bluesky_template$::jsonb,
    schema_version = 1
WHERE code = 'bluesky';

UPDATE feed_templates
SET content = $truth_social_template$
{
  "ROOT": {
    "type": {
      "resolvedName": "TruthSocialTemplate"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Truth Social Template",
    "custom": {},
    "hidden": false,
    "nodes": [],
    "linkedNodes": {
      "truth-social-root": "rEp_CyIWfm"
    }
  },
  "rEp_CyIWfm": {
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
      "PSn0UzDkiU"
    ],
    "linkedNodes": {}
  },
  "PSn0UzDkiU": {
    "type": {
      "resolvedName": "TruthSocialDesktopShell"
    },
    "isCanvas": true,
    "props": {
      "id": "truth-social-shell"
    },
    "displayName": "Truth Social Desktop Shell",
    "custom": {},
    "parent": "rEp_CyIWfm",
    "hidden": false,
    "nodes": [
      "fYwFWCSl3h"
    ],
    "linkedNodes": {}
  },
  "fYwFWCSl3h": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex h-full w-full bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "PSn0UzDkiU",
    "hidden": false,
    "nodes": [
      "DK9jF7l-Wh",
      "5cEjICHk0y",
      "4NWAENCxAI",
      "Uz4E1KcEB0"
    ],
    "linkedNodes": {}
  },
  "DK9jF7l-Wh": {
    "type": {
      "resolvedName": "TruthSocialSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Truth Social Sidebar",
    "custom": {},
    "parent": "fYwFWCSl3h",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "5cEjICHk0y": {
    "type": {
      "resolvedName": "TruthSocialMainFeed"
    },
    "isCanvas": true,
    "props": {
      "id": "truth-social-main-feed"
    },
    "displayName": "Truth Social Main Feed",
    "custom": {},
    "parent": "fYwFWCSl3h",
    "hidden": false,
    "nodes": [
      "J2m9WI2BWD",
      "iya6murd87",
      "QULuAv9wp8"
    ],
    "linkedNodes": {}
  },
  "J2m9WI2BWD": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "sticky top-0 z-20 grid h-[58px] grid-cols-[1fr_1fr_0.85fr] border-b border-[#e6eaf0] bg-white/95 backdrop-blur"
    },
    "displayName": "div",
    "custom": {},
    "parent": "5cEjICHk0y",
    "hidden": false,
    "nodes": [
      "1xmwHmgfIS",
      "IofycQKz7f",
      "IUD35gDRQL"
    ],
    "linkedNodes": {}
  },
  "1xmwHmgfIS": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "relative flex items-center justify-center"
    },
    "displayName": "div",
    "custom": {},
    "parent": "J2m9WI2BWD",
    "hidden": false,
    "nodes": [
      "7MvdX9K-GU",
      "JDPgzoCyNc"
    ],
    "linkedNodes": {}
  },
  "7MvdX9K-GU": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "text-[14px] font-semibold text-[#ef1f2c]",
      "children": "For You"
    },
    "displayName": "span",
    "custom": {},
    "parent": "1xmwHmgfIS",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "JDPgzoCyNc": {
    "type": "span",
    "isCanvas": false,
    "props": {
      "className": "absolute bottom-0 h-[3px] w-[48px] rounded-full bg-[#ef1f2c]"
    },
    "displayName": "span",
    "custom": {},
    "parent": "1xmwHmgfIS",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "IofycQKz7f": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex items-center justify-center text-[14px] font-semibold text-[#687386]",
      "children": "Following"
    },
    "displayName": "div",
    "custom": {},
    "parent": "J2m9WI2BWD",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "IUD35gDRQL": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "flex items-center justify-center text-[13px] font-medium text-[#9aa4b3]",
      "children": "Groups"
    },
    "displayName": "div",
    "custom": {},
    "parent": "J2m9WI2BWD",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "iya6murd87": {
    "type": {
      "resolvedName": "TruthSocialComposer"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Truth Social Composer",
    "custom": {},
    "parent": "5cEjICHk0y",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "QULuAv9wp8": {
    "type": {
      "resolvedName": "TruthSocialPostList"
    },
    "isCanvas": true,
    "props": {
      "id": "truth-social-post-list"
    },
    "displayName": "Truth Social Post List",
    "custom": {},
    "parent": "5cEjICHk0y",
    "hidden": false,
    "nodes": [
      "emrnRrzeG8",
      "SQymI_Pea2"
    ],
    "linkedNodes": {}
  },
  "emrnRrzeG8": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "truth-social",
      "username": "@emmaw",
      "displayName": "Emma Wilson",
      "time": "2h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Beautiful weather for a walk around campus today. The autumn leaves are absolutely stunning this year 🍂",
      "likes": 47,
      "comments": 12,
      "shares": 8,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "QULuAv9wp8",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "SQymI_Pea2": {
    "type": {
      "resolvedName": "PostWidget"
    },
    "isCanvas": false,
    "props": {
      "styleId": "truth-social",
      "username": "@alexc",
      "displayName": "Alex Chen",
      "time": "4h ago",
      "avatarSrc": "",
      "imageSrc": "",
      "caption": "Working on an interesting social media research project. The differences in UI patterns across platforms are far more significant than I expected. #research #HCI #UXDesign",
      "likes": 83,
      "comments": 21,
      "shares": 34,
      "views": 1243,
      "saves": 5,
      "sound": "Original Sound · Emma Wilson"
    },
    "displayName": "Post Widget",
    "custom": {},
    "parent": "QULuAv9wp8",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "4NWAENCxAI": {
    "type": {
      "resolvedName": "TruthSocialRightSidebar"
    },
    "isCanvas": false,
    "props": {},
    "displayName": "Truth Social Right Sidebar",
    "custom": {},
    "parent": "fYwFWCSl3h",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  },
  "Uz4E1KcEB0": {
    "type": "div",
    "isCanvas": false,
    "props": {
      "className": "min-w-0 flex-1 bg-white"
    },
    "displayName": "div",
    "custom": {},
    "parent": "fYwFWCSl3h",
    "hidden": false,
    "nodes": [],
    "linkedNodes": {}
  }
}
$truth_social_template$::jsonb,
    schema_version = 1
WHERE code = 'truth-social';
